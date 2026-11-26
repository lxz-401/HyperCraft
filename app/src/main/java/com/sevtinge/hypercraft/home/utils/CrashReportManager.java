package com.sevtinge.hypercraft.home.utils;

import android.app.Activity;

import com.sevtinge.hypercraft.R;
import com.sevtinge.hypercraft.libhook.safecrash.CrashScope;
import com.sevtinge.hypercraft.utils.DialogHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class CrashReportManager {
    private static final Map<String, Integer> NAME_MAP = Map.of(
        "com.android.systemui", com.sevtinge.hypercraft.core.R.string.system_ui,
        "com.android.settings", com.sevtinge.hypercraft.core.R.string.system_settings,
        "com.miui.home", com.sevtinge.hypercraft.core.R.string.mihome,
        "com.miui.securitycenter", com.sevtinge.hypercraft.core.R.string.security_center_hyperos
    );

    public static List<String> getCrashList() {
        try {
            List<?> raw = CrashScope.getCrashingPackages();
            return raw.stream().map(Object::toString).toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static void handleSafeMode(Activity activity, List<String> crashes) {
        if (crashes.isEmpty()) return;

        StringJoiner sj = new StringJoiner(", ");
        for (String pkg : crashes) {
            Integer res = NAME_MAP.get(pkg);
            if (res != null) sj.add(activity.getString(res) + " (" + pkg + ")");
        }

        String names = sj.toString();
        if (names.isEmpty()) return;

        String msg = clean(activity.getString(R.string.safe_mode_later_desc, names));
        DialogHelper.showSafeModeDialog(activity, msg);
    }

    private static String clean(String s) {
        return s.replaceAll("[\\[\\]]", "").replaceAll("\\s{2,}", " ").trim();
    }
}
