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

import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.Miui.isPad;

import com.hchen.database.HookBase;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.contentextension.DoublePress;
import com.sevtinge.hypercraft.libhook.rules.contentextension.LinkOpenMode;
import com.sevtinge.hypercraft.libhook.rules.contentextension.Taplus;
import com.sevtinge.hypercraft.libhook.rules.contentextension.UnlockTaplus;
import com.sevtinge.hypercraft.libhook.rules.contentextension.UseThirdPartyBrowser;

@HookBase(targetPackage = "com.miui.contentextension")
public class ContentExtension extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new UseThirdPartyBrowser(), PrefsBridge.getBoolean("content_extension_browser"));
        initHook(new DoublePress(), PrefsBridge.getBoolean("content_extension_double_press"));
        initHook(new Taplus(), PrefsBridge.getBoolean("security_center_taplus"));
        initHook(new LinkOpenMode(), true);
        initHook(UnlockTaplus.INSTANCE, PrefsBridge.getBoolean("content_extension_unlock_taplus") && isPad());
    }
}
