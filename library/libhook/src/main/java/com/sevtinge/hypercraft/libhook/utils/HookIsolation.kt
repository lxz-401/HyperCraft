package com.sevtinge.hypercraft.libhook.utils

import com.sevtinge.hypercraft.common.log.XposedLog
import com.sevtinge.hypercraft.libhook.safecrash.SafeModeHandler

/**
 * Hook isolation utility for wrapping feature initialization with try-catch.
 *
 * Prevents a single feature hook failure from cascading to other hooks or
 * crashing the host application (SystemUI, Home, etc.).
 *
 * Usage:
 * ```kotlin
 * object MyFeatureHook : BaseHook() {
 *     override fun init() {
 *         HookIsolation.safeInit("MyFeatureHook") {
 *             // Feature-specific initialization
 *         }
 *     }
 * }
 * ```
 */
object HookIsolation {

    private const val TAG = "HookIsolation"

    /**
     * Safely initialize a feature hook.
     *
     * @param featureName Feature name for logging
     * @param initBlock Initialization code to execute
     * @return true if initialization succeeded, false if failed
     */
    fun safeInit(featureName: String, initBlock: () -> Unit): Boolean {
        return try {
            initBlock()
            XposedLog.d(TAG, "$featureName initialized successfully")
            true
        } catch (e: NoClassDefFoundError) {
            handleInitFailure(featureName, "Class not found", e)
            false
        } catch (e: NoSuchMethodException) {
            handleInitFailure(featureName, "Method not found", e)
            false
        } catch (e: NoSuchFieldException) {
            handleInitFailure(featureName, "Field not found", e)
            false
        } catch (e: Exception) {
            handleInitFailure(featureName, "Exception", e)
            false
        } catch (e: Throwable) {
            handleInitFailure(featureName, "Fatal", e)
            false
        }
    }

    /**
     * Safely execute a hook callback (before/after).
     *
     * @param featureName Feature name for logging
     * @param callback Hook callback to execute
     * @return true if callback completed successfully
     */
    fun safeCallback(featureName: String, callback: () -> Unit): Boolean {
        return try {
            callback()
            true
        } catch (e: Exception) {
            XposedLog.w(TAG, "$featureName callback failed: ${e.message}", e)
            SafeModeHandler.reportCrash(featureName, e)
            false
        } catch (e: Throwable) {
            XposedLog.w(TAG, "$featureName callback fatal: ${e.message}", e)
            SafeModeHandler.reportCrash(featureName, e)
            false
        }
    }

    /**
     * Check if a class exists before attempting to hook it.
     *
     * @param className Fully qualified class name
     * @param classLoader Class loader to use
     * @return true if class is found
     */
    fun classExists(className: String, classLoader: ClassLoader): Boolean {
        return try {
            classLoader.loadClass(className)
            true
        } catch (e: ClassNotFoundException) {
            XposedLog.d(TAG, "Class not found: $className")
            false
        }
    }

    /**
     * Log a warning for a failed hook installation and optionally report to SafeModeHandler.
     */
    private fun handleInitFailure(featureName: String, reason: String, e: Throwable) {
        XposedLog.w(TAG, "$featureName failed to initialize ($reason): ${e.message}", e)
        // Only report critical failures to SafeModeHandler
        if (e is NoClassDefFoundError || e is ExceptionInInitializerError) {
            SafeModeHandler.reportCrash(featureName, e)
        }
    }

    /**
     * Safe version check for a required Android SDK level.
     *
     * @param requiredSdk Minimum SDK version
     * @param featureName Feature name for logging
     * @return true if current SDK meets the requirement
     */
    fun checkMinSdk(requiredSdk: Int, featureName: String): Boolean {
        val currentSdk = android.os.Build.VERSION.SDK_INT
        if (currentSdk < requiredSdk) {
            XposedLog.w(TAG, "$featureName requires SDK $requiredSdk, current is $currentSdk")
            return false
        }
        return true
    }

    /**
     * Run a block only if all provided class names are found.
     *
     * @param classNames Required class names
     * @param classLoader Class loader
     * @param block Code to execute
     */
    fun ifClassesExist(vararg classNames: String, classLoader: ClassLoader, block: () -> Unit) {
        for (className in classNames) {
            if (!classExists(className, classLoader)) {
                return
            }
        }
        block()
    }
}