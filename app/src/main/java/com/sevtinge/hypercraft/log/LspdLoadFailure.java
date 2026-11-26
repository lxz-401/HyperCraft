/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hypercraft.log;

/** Framework-side APK failures happen before the module can emit its own logs. */
final class LspdLoadFailure {
    private LspdLoadFailure() {}

    static boolean matches(String header) {
        // Require our APK path, not another module mentioning HyperCraft in a message.
        if (header == null || !header.contains("/com.sevtinge.hypercraft-")
            || !header.contains("/base.apk")) return false;
        return header.contains("Unable to open '") || header.contains("Failed to open APK '")
            || header.contains("failed to parse ") || header.contains("Failed to parse ");
    }

    static String explain(String message) {
        return "MODULE_APK_LOAD_FAILED: " + message
            + "\nLSPosed could not read the HyperCraft APK during process startup. "
            + "The affected process may have skipped module hooks; Dock background requires "
            + "the system_server hook. Verify module scope and APK availability, then restart "
            + "the device and refresh logs. Restarting the launcher alone cannot reload system_server.";
    }
}
