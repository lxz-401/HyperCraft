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
import com.sevtinge.hypercraft.libhook.rules.milink.AllowCameraDevices;
import com.sevtinge.hypercraft.libhook.rules.milink.DisablePcManagerClipboardRegionCheck;
import com.sevtinge.hypercraft.libhook.rules.milink.FuckHpplay;
import com.sevtinge.hypercraft.libhook.rules.milink.UnlockMiShare;
import com.sevtinge.hypercraft.libhook.rules.milink.UnlockSendApp;

@HookBase(targetPackage = "com.milink.service")
public class MiLink extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new UnlockMiShare(), PrefsBridge.getBoolean("milink_unlock_mishare"));
        initHook(UnlockSendApp.INSTANCE, PrefsBridge.getBoolean("milink_unlock_send_app"));
        initHook(new DisablePcManagerClipboardRegionCheck(), PrefsBridge.getBoolean("milink_disable_pc_clipboard_region_check"));
        initHook(new AllowCameraDevices(), PrefsBridge.getBoolean("milink_allow_camera_devices"));
        initHook(new FuckHpplay(), PrefsBridge.getBoolean("milink_fuck_hpplay"));
    }
}
