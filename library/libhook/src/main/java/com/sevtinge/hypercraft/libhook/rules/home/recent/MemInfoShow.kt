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
package com.sevtinge.hypercraft.libhook.rules.home.recent

import com.sevtinge.hypercraft.common.log.XposedLog
import com.sevtinge.hypercraft.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object MemInfoShow : BaseHook() {
    override fun init() {
        // hyperOS for Pad 修复方案来自 hyper helper
        runCatching {
            // 此方法调用会将内存显示 hide，需拦截
            loadClass("com.miui.home.recents.views.RecentsDecorations").findMethod { name("hideTxtMemoryInfoView") }.createHook {
                returnConstant(null)
            }
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName, "hideTxtMemoryInfoView method is null")
        }

        try {
            loadClass("com.miui.home.recents.views.RecentsDecorations").findMethod { name("isMemInfoShow") }
        } catch (_: Throwable) {
            loadClass("com.miui.home.recents.views.RecentsDecorations").findMethod { name("canTxtMemInfoShow") }
        }.createHook {
            returnConstant(true)
        }
    }
}
