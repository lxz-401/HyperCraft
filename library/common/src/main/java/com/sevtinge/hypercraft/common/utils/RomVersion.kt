package com.sevtinge.hypercraft.common.utils

import android.os.Build
import com.sevtinge.hypercraft.common.log.XposedLog

/**
 * ROM version abstraction for HyperOS/MIUI version detection and compatibility checks.
 * Centralizes all version parsing logic to avoid scattered Build.VERSION checks.
 */
data class RomVersion(
    val androidSdk: Int = Build.VERSION.SDK_INT,
    val androidRelease: String = Build.VERSION.RELEASE,
    val androidIncremental: String = Build.VERSION.INCREMENTAL,
    val hyperOsVersion: String? = getHyperOsVersion(),
    val miuiVersion: String? = getMiuiVersion(),
    val fingerprint: String = Build.FINGERPRINT,
    val device: String = Build.DEVICE,
    val manufacturer: String = Build.MANUFACTURER,
    val brand: String = Build.BRAND,
    val model: String = Build.MODEL,
    val product: String = Build.PRODUCT
) {

    /** Major Android version (e.g., 14, 15, 16) */
    val androidMajor: Int
        get() = when {
            androidSdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> 15 // Android 15
            androidSdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> 14 // Android 14
            androidSdk >= Build.VERSION_CODES.TIRAMISU -> 13
            androidSdk >= Build.VERSION_CODES.S_V2 -> 12
            androidSdk >= Build.VERSION_CODES.R -> 11
            else -> androidSdk
        }

    /** Check if running on HyperOS (vs MIUI) */
    val isHyperOs: Boolean
        get() = hyperOsVersion != null

    /** Check if running on MIUI */
    val isMiui: Boolean
        get() = miuiVersion != null

    /** HyperOS major version (e.g., 1, 2) or null */
    val hyperOsMajor: Int?
        get() = hyperOsVersion?.split('.')?.firstOrNull()?.toIntOrNull()

    /** MIUI version code (e.g., 14.0.5 -> 14005) or null */
    val miuiVersionCode: Int?
        get() = miuiVersion?.replace(".", "")?.toIntOrNull()

    /** Check if Android version is at least the given SDK */
    fun isAtLeastAndroid(sdk: Int): Boolean = androidSdk >= sdk

    /** Check if Android version is exactly the given SDK */
    fun isAndroid(sdk: Int): Boolean = androidSdk == sdk

    /** Check if HyperOS version is at least the given major version */
    fun isAtLeastHyperOs(major: Int): Boolean = hyperOsMajor != null && hyperOsMajor!! >= major

    /** Check if MIUI version is at least the given version code */
    fun isAtLeastMiui(versionCode: Int): Boolean = miuiVersionCode != null && miuiVersionCode!! >= versionCode

    /** Check if ROM is Xiaomi-based (HyperOS or MIUI) */
    val isXiaomiRom: Boolean
        get() = isHyperOs || isMiui || manufacturer.equals("Xiaomi", ignoreCase = true) || brand.equals("Xiaomi", ignoreCase = true)

    /** Check if ROM is officially supported (Xiaomi HyperOS/MIUI) */
    val isSupportedRom: Boolean
        get() = isXiaomiRom && (isAtLeastAndroid(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) || isAtLeastMiui(14000))

    /** Get a human-readable version string for logging */
    override fun toString(): String {
        return "RomVersion(android=$androidMajor (SDK $androidSdk), hyperOs=$hyperOsVersion, miui=$miuiVersion, device=$device, fp=$fingerprint)"
    }

    companion object {
        /** Singleton instance - lazily initialized */
        private var INSTANCE: RomVersion? = null

        /** Get the current ROM version (cached) */
        fun current(): RomVersion {
            return INSTANCE ?: RomVersion().also { INSTANCE = it }
        }

        /** Force re-detection (for testing) */
        fun reset() {
            INSTANCE = null
        }

        private fun getHyperOsVersion(): String? {
            return try {
                val prop = Class.forName("android.os.SystemProperties")
                val get = prop.getMethod("get", String::class.java, String::class.java)
                get.invoke(null, "ro.build.version.hyper_os", "") as String?
                    ?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }

        private fun getMiuiVersion(): String? {
            return try {
                val prop = Class.forName("android.os.SystemProperties")
                val get = prop.getMethod("get", String::class.java, String::class.java)
                get.invoke(null, "ro.miui.version", "") as String?
                    ?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                // Fallback to Build.VERSION.INCREMENTAL if it looks like MIUI
                Build.VERSION.INCREMENTAL.takeIf { it.contains("MIUI", ignoreCase = true) }
            }
        }
    }
}

/**
 * Extension functions for common version checks
 */
fun RomVersion.isAndroid14Plus(): Boolean = isAtLeastAndroid(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun RomVersion.isAndroid15Plus(): Boolean = isAtLeastAndroid(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun RomVersion.isHyperOs1Plus(): Boolean = isAtLeastHyperOs(1)
fun RomVersion.isHyperOs2Plus(): Boolean = isAtLeastHyperOs(2)
fun RomVersion.isMiui14Plus(): Boolean = isAtLeastMiui(14000)

/**
 * Version gating helper for hooks
 */
object VersionGate {
    /** Require minimum Android SDK */
    fun requireAndroid(minSdk: Int, feature: String): Boolean {
        if (!RomVersion.current().isAtLeastAndroid(minSdk)) {
            XposedLog.w("VersionGate", "$feature requires Android SDK $minSdk, current is ${Build.VERSION.SDK_INT}")
            return false
        }
        return true
    }

    /** Require minimum HyperOS major version */
    fun requireHyperOs(minMajor: Int, feature: String): Boolean {
        if (!RomVersion.current().isAtLeastHyperOs(minMajor)) {
            XposedLog.w("VersionGate", "$feature requires HyperOS $minMajor+, current is ${RomVersion.current().hyperOsVersion}")
            return false
        }
        return true
    }

    /** Require minimum MIUI version code */
    fun requireMiui(minVersionCode: Int, feature: String): Boolean {
        if (!RomVersion.current().isAtLeastMiui(minVersionCode)) {
            XposedLog.w("VersionGate", "$feature requires MIUI $minVersionCode+, current is ${RomVersion.current().miuiVersion}")
            return false
        }
        return true
    }

    /** Require supported Xiaomi ROM */
    fun requireXiaomiRom(feature: String): Boolean {
        if (!RomVersion.current().isSupportedRom) {
            XposedLog.w("VersionGate", "$feature requires supported Xiaomi ROM, current: ${RomVersion.current()}")
            return false
        }
        return true
    }
}