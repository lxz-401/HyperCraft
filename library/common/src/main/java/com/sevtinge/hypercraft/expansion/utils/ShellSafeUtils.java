package com.sevtinge.hypercraft.expansion.utils;

public class ShellSafeUtils {
    public static boolean isSafeCommand(String cmd) {
        return cmd != null && !cmd.trim().isEmpty();
    }
}
