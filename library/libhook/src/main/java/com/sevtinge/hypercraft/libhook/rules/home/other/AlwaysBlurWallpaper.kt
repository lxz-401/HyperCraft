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
package com.sevtinge.hypercraft.libhook.rules.home.other

import com.sevtinge.hypercraft.common.utils.PrefsBridge
import com.sevtinge.hypercraft.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object AlwaysBlurWallpaper : BaseHook() {
    private val value by lazy {
        PrefsBridge.getInt("home_blur_radius", 100)
    }

    override fun init() {
        loadClass("com.miui.home.launcher.common.BlurUtils").findMethod { name("fastBlur"); paramCount(4) }.createBeforeHook {
                it.args[0] = value.toFloat() / 100
                it.args[2] = true
            }
    }
}
