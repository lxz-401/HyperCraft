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
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.barrage.AnyBarrage;
import com.sevtinge.hypercraft.libhook.rules.barrage.BarrageNotTouchable;
import com.sevtinge.hypercraft.libhook.rules.barrage.CustomBarrageLength;
import com.sevtinge.hypercraft.libhook.rules.barrage.GlobalBarrage;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;

@HookBase(targetPackage = "com.xiaomi.barrage")
public class Barrage extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(AnyBarrage.INSTANCE, PrefsBridge.getBoolean("barrage_any_barrage"));
        initHook(BarrageNotTouchable.INSTANCE, PrefsBridge.getBoolean("barrage_not_touchable"));
        initHook(GlobalBarrage.INSTANCE, PrefsBridge.getBoolean("barrage_global_enable"));
        int length = PrefsBridge.getInt("barrage_custom_barrage_length", 36);
        initHook(CustomBarrageLength.INSTANCE, length >= 12 && length <= 108 && length != 36);
    }
}
