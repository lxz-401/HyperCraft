package com.sevtinge.hypercraft.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RomVersionTest {

    @Test
    fun androidMajor() {
        assertEquals(14, RomVersion(34, "14", "test", null, null, "", "", "", "", "", "").androidMajor)
        assertEquals(13, RomVersion(33, "13", "test", null, null, "", "", "", "", "", "").androidMajor)
        assertEquals(12, RomVersion(31, "12", "test", null, null, "", "", "", "", "", "").androidMajor)
    }

    @Test
    fun isAtLeastAndroid() {
        val version = RomVersion(34, "14", "test", null, null, "", "", "", "", "", "")
        assertTrue(version.isAtLeastAndroid(33))
        assertTrue(version.isAtLeastAndroid(34))
        assertFalse(version.isAtLeastAndroid(35))
    }

    @Test
    fun isAtLeastHyperOs() {
        val v2 = RomVersion(34, "14", "test", "2.0.1", null, "", "", "", "", "", "")
        assertTrue(v2.isAtLeastHyperOs(1))
        assertTrue(v2.isAtLeastHyperOs(2))
        assertFalse(v2.isAtLeastHyperOs(3))

        val v1 = RomVersion(34, "14", "test", "1.0.0", null, "", "", "", "", "", "")
        assertTrue(v1.isAtLeastHyperOs(1))
        assertFalse(v1.isAtLeastHyperOs(2))
    }

    @Test
    fun isAtLeastMiui() {
        val v14 = RomVersion(33, "13", "test", null, "14.0.5", "", "", "", "", "", "")
        assertTrue(v14.isAtLeastMiui(140000))
        assertFalse(v14.isAtLeastMiui(140006))

        val v13 = RomVersion(33, "13", "test", null, "13.0.9", "", "", "", "", "", "")
        assertTrue(v13.isAtLeastMiui(130000))
        assertFalse(v13.isAtLeastMiui(140000))
    }

    @Test
    fun isXiaomiRom() {
        // HyperOS
        val hyper = RomVersion(34, "14", "test", "2.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertTrue(hyper.isXiaomiRom)

        // MIUI
        val miui = RomVersion(33, "13", "test", null, "14.0", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertTrue(miui.isXiaomiRom)

        // Manufacturer Xiaomi
        val manufacturer = RomVersion(33, "13", "test", null, null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertTrue(manufacturer.isXiaomiRom)

        // Non-Xiaomi
        val google = RomVersion(33, "13", "test", null, null, "", "", "Google", "Google", "Google", "")
        assertFalse(google.isXiaomiRom)
    }

    @Test
    fun isSupportedRom() {
        // HyperOS on Android 14+
        val supportedHyper = RomVersion(34, "14", "test", "2.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertTrue(supportedHyper.isSupportedRom)

        // MIUI 14+ on Android 13+
        val supportedMiui = RomVersion(33, "13", "test", null, "14.0.5", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertTrue(supportedMiui.isSupportedRom)

        // Too old Android
        val oldAndroid = RomVersion(31, "12", "test", "1.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertFalse(oldAndroid.isSupportedRom)

        // Too old MIUI
        val oldMiui = RomVersion(33, "13", "test", null, "13.0.0", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertFalse(oldMiui.isSupportedRom)

        // Non-Xiaomi
        val nonXiaomi = RomVersion(34, "14", "test", null, null, "", "", "Google", "Google", "Google", "")
        assertFalse(nonXiaomi.isSupportedRom)
    }

    @Test
    fun convenienceMethods() {
        val v14 = RomVersion(34, "14", "test", "2.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertTrue(v14.isAndroid14Plus())
        assertFalse(v14.isAndroid15Plus())
        assertTrue(v14.isHyperOs1Plus())
        assertTrue(v14.isHyperOs2Plus())
        assertFalse(v14.isMiui14Plus())

        // SDK 33 is Android 13: not 14+. MIUI "14.0.0" -> version code 14000.
        val v13 = RomVersion(33, "13", "test", null, "14.0.0", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "")
        assertFalse(v13.isAndroid14Plus())
        assertFalse(v13.isAndroid15Plus())
        assertFalse(v13.isHyperOs1Plus())
        assertFalse(v13.isHyperOs2Plus())
        assertTrue(v13.isMiui14Plus())
    }

    @Test
    fun toStringIncludesKeyFields() {
        val version = RomVersion(
            34, "14", "test_inc", "2.0.1", null,
            "fingerprint", "device", "Xiaomi", "Xiaomi", "model", "product"
        )
        val str = version.toString()
        assertTrue(str.contains("android=14"))
        assertTrue(str.contains("hyperOs=2.0.1"))
        assertTrue(str.contains("device=device"))
        assertTrue(str.contains("fp=fingerprint"))
    }
}
