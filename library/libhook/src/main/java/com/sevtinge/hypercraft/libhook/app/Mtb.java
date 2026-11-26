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
import com.sevtinge.hypercraft.libhook.rules.mtb.BypassAuthentication;
import com.sevtinge.hypercraft.libhook.rules.mtb.IsUserBuild;

@HookBase(targetPackage = "com.xiaomi.mtb")
public class Mtb extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        initHook(BypassAuthentication.INSTANCE, PrefsBridge.getBoolean("mtb_auth"));
        initHook(IsUserBuild.INSTANCE, PrefsBridge.getBoolean("mtb_auth"));
    }
}
