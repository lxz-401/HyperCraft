package com.sevtinge.hypercraft.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.aiasst.NewAiCaptions;
import com.sevtinge.hypercraft.libhook.rules.aiasst.UnlockSplitTranslation;

@HookBase(targetPackage = "com.xiaomi.aiasst.vision")
public class AiAsst extends BaseLoad {


    @Override
    public void onPackageLoaded() {
        initHook(NewAiCaptions.INSTANCE, PrefsBridge.getBoolean("aiasst_ai_captions"));
        initHook(UnlockSplitTranslation.INSTANCE, PrefsBridge.getBoolean("aiasst_unlock_split_screen_translation"));
    }
}
