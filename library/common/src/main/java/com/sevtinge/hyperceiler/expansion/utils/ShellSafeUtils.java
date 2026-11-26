package com.sevtinge.hyperceiler.expansion.utils;

public class ShellSafeUtils {
    public static boolean isSafeCommand(String cmd) {
        return cmd != null && !cmd.trim().isEmpty();
    }
}
