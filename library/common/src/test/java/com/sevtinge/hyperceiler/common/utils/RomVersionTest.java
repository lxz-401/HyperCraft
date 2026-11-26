package com.sevtinge.hyperceiler.common.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RomVersionTest {

    @Test
    public void testAndroidMajor() {
        // These test the logic but actual values depend on build environment
        RomVersion version = new RomVersion(34, "14", "test", null, null, "", "", "", "", "", "");
        assertEquals(14, version.androidMajor);

        version = new RomVersion(33, "13", "test", null, null, "", "", "", "", "", "");
        assertEquals(13, version.androidMajor);

        version = new RomVersion(31, "12", "test", null, null, "", "", "", "", "", "");
        assertEquals(12, version.androidMajor);
    }

    @Test
    public void testIsAtLeastAndroid() {
        RomVersion version = new RomVersion(34, "14", "test", null, null, "", "", "", "", "", "");
        assertTrue(version.isAtLeastAndroid(33));
        assertTrue(version.isAtLeastAndroid(34));
        assertFalse(version.isAtLeastAndroid(35));
    }

    @Test
    public void testIsAtLeastHyperOs() {
        RomVersion version = new RomVersion(34, "14", "test", "2.0.1", null, "", "", "", "", "", "");
        assertTrue(version.isAtLeastHyperOs(1));
        assertTrue(version.isAtLeastHyperOs(2));
        assertFalse(version.isAtLeastHyperOs(3));

        version = new RomVersion(34, "14", "test", "1.0.0", null, "", "", "", "", "", "");
        assertTrue(version.isAtLeastHyperOs(1));
        assertFalse(version.isAtLeastHyperOs(2));
    }

    @Test
    public void testIsAtLeastMiui() {
        RomVersion version = new RomVersion(33, "13", "test", null, "14.0.5", "", "", "", "", "", "");
        assertTrue(version.isAtLeastMiui(14000));
        assertFalse(version.isAtLeastMiui(14006));

        version = new RomVersion(33, "13", "test", null, "13.0.9", "", "", "", "", "", "");
        assertTrue(version.isAtLeastMiui(13000));
        assertFalse(version.isAtLeastMiui(14000));
    }

    @Test
    public void testIsXiaomiRom() {
        // HyperOS
        RomVersion version = new RomVersion(34, "14", "test", "2.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isXiaomiRom);

        // MIUI
        version = new RomVersion(33, "13", "test", null, "14.0", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isXiaomiRom);

        // Manufacturer Xiaomi
        version = new RomVersion(33, "13", "test", null, null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isXiaomiRom);

        // Non-Xiaomi
        version = new RomVersion(33, "13", "test", null, null, "", "", "Google", "Google", "Google", "");
        assertFalse(version.isXiaomiRom);
    }

    @Test
    public void testIsSupportedRom() {
        // HyperOS on Android 14+
        RomVersion version = new RomVersion(34, "14", "test", "2.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isSupportedRom);

        // MIUI 14+ on Android 13+
        version = new RomVersion(33, "13", "test", null, "14.0.5", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isSupportedRom);

        // Too old Android
        version = new RomVersion(31, "12", "test", "1.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertFalse(version.isSupportedRom);

        // Too old MIUI
        version = new RomVersion(33, "13", "test", null, "13.0.0", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertFalse(version.isSupportedRom);

        // Non-Xiaomi
        version = new RomVersion(34, "14", "test", null, null, "", "", "Google", "Google", "Google", "");
        assertFalse(version.isSupportedRom);
    }

    @Test
    public void testConvenienceMethods() {
        RomVersion version = new RomVersion(34, "14", "test", "2.0", null, "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isAndroid14Plus());
        assertFalse(version.isAndroid15Plus());
        assertTrue(version.isHyperOs1Plus());
        assertTrue(version.isHyperOs2Plus());
        assertFalse(version.isMiui14Plus());

        version = new RomVersion(33, "13", "test", null, "14.0", "", "", "Xiaomi", "Xiaomi", "Xiaomi", "");
        assertTrue(version.isAndroid14Plus()); // SDK 33 is Android 13, not 14
        assertFalse(version.isAndroid15Plus());
        assertFalse(version.isHyperOs1Plus());
        assertFalse(version.isHyperOs2Plus());
        assertTrue(version.isMiui14Plus());
    }

    @Test
    public void testToString() {
        RomVersion version = new RomVersion(34, "14", "test_inc", "2.0.1", null, "fingerprint", "device", "Xiaomi", "Xiaomi", "model", "product");
        String str = version.toString();
        assertTrue(str.contains("android=14"));
        assertTrue(str.contains("hyperOs=2.0.1"));
        assertTrue(str.contains("device=device"));
        assertTrue(str.contains("fp=fingerprint"));
    }
}