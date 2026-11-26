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
import com.sevtinge.hypercraft.common.log.XposedLog;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.lbe.AutoStart;
import com.sevtinge.hypercraft.libhook.rules.lbe.PasteToast;


@HookBase(targetPackage = "com.lbe.security.miui")
public class Lbe extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new AutoStart(), PrefsBridge.getBoolean("lbe_auto_start"));

        // paste toast
        initHook(new PasteToast(), PrefsBridge.getBoolean("lbe_paste_toast_custom"));


    }
}
