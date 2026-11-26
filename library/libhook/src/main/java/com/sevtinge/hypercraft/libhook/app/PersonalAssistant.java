/*
 * This file is part of HyperCraft.

 * HyperCraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCraft Contributions
 */
package com.sevtinge.hypercraft.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.personalassistant.DisableLiteVersion;
import com.sevtinge.hypercraft.libhook.rules.personalassistant.ClearGadgetBridge;
import com.sevtinge.hypercraft.libhook.rules.personalassistant.UnlockWidgetCountLimit;
import com.sevtinge.hypercraft.libhook.rules.personalassistant.WidgetBlurOpt;

@HookBase(targetPackage = "com.miui.personalassistant")
public class PersonalAssistant extends BaseLoad {


    @Override
    public void onPackageLoaded() {
        // Home's guarded native serializer emits the private marker only while its switch is on.
        // Do not gate this receiver on a stale startup preference snapshot.
        initHook(new ClearGadgetBridge(),
            com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.isHyperOSVersion(4f));
        // initHook(new BlurOverlay(), false);
        initHook(new DisableLiteVersion(), PrefsBridge.getBoolean("personal_assistant_disable_lite_version"));
        initHook(new UnlockWidgetCountLimit(), PrefsBridge.getBoolean("personal_assistant_unlock_widget_count_limit"));

        initHook(new WidgetBlurOpt(), PrefsBridge.getBoolean("personal_assistant_widget_widget_blur_opt"));
    }

}
