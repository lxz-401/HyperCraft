package com.sevtinge.hypercraft.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import com.sevtinge.hypercraft.common.log.XposedLog;
import com.sevtinge.hypercraft.libhook.base.PackageTarget;

public class VerificationAgentPatch extends CorePatchHelper {

    private static final String TAG = "VerificationAgentPatch";

    public void init(PackageTarget lpparam) {
        // Android 11+
        try {
            hookAllMethods(getIsVerificationEnabledClass(lpparam.getClassLoader()), "isVerificationEnabled", new ReturnConstant("prefs_key_system_framework_disable_verification_agent", false));
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ hook failed, crash: " + t);
        }
    }

    Class<?> getIsVerificationEnabledClass(ClassLoader classLoader) {
        if (isMoreAndroidVersion(34)) {
            return com.sevtinge.hypercraft.libhook.base.BaseHook.findClass("com.android.server.pm.VerifyingSession", classLoader);
        } else if (isAndroidVersion(33)) {
            return com.sevtinge.hypercraft.libhook.base.BaseHook.findClass("com.android.server.pm.VerificationParams", classLoader);
        }
        return com.sevtinge.hypercraft.libhook.base.BaseHook.findClass("com.android.server.pm.PackageManagerService", classLoader);
    }
}
