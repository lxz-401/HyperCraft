package com.sevtinge.hypercraft.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.photopicker.DisableReroute;

@HookBase(targetPackage = "com.android.photopicker")
public class PhotoPicker extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        initHook(new DisableReroute(), PrefsBridge.getBoolean("photopicker_disable_reroute"));
    }
}
