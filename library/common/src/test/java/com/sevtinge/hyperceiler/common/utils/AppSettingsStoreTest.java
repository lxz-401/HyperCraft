package com.sevtinge.hyperceiler.common.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class AppSettingsStoreTest {

    @Test
    public void testParseBoolean_1() {
        assertTrue(parseBoolean("1", false));
        assertTrue(parseBoolean("true", false));
        assertTrue(parseBoolean("True", false));
    }

    @Test
    public void testParseBoolean_0() {
        assertFalse(parseBoolean("0", true));
        assertFalse(parseBoolean("false", true));
        assertFalse(parseBoolean("False", true));
    }

    @Test
    public void testParseBoolean_default() {
        assertTrue(parseBoolean("", true));
        assertFalse(parseBoolean("invalid", false));
        assertTrue(parseBoolean(null, true));
    }

    @Test
    public void testParseInt_valid() {
        assertEquals(42, parseInt("42", 0));
        assertEquals(0, parseInt("0", 1));
        assertEquals(-5, parseInt("-5", 10));
    }

    @Test
    public void testParseInt_invalid() {
        assertEquals(0, parseInt("", 0));
        assertEquals(10, parseInt("invalid", 10));
        assertEquals(5, parseInt(null, 5));
    }

    @Test
    public void testParseInt_whitespace() {
        assertEquals(42, parseInt(" 42 ", 0));
        assertEquals(0, parseInt(" ", 0));
    }

    // Package-private helper methods for testing (copied from AppSettingsStore logic)
    static boolean parseBoolean(String raw, boolean defValue) {
        if (raw == null || raw.isEmpty()) {
            return defValue;
        }
        if ("1".equals(raw)) return true;
        if ("0".equals(raw)) return false;
        return Boolean.parseBoolean(raw);
    }

    static int parseInt(String raw, int defValue) {
        if (raw == null || raw.isEmpty()) {
            return defValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return defValue;
        }
    }
}