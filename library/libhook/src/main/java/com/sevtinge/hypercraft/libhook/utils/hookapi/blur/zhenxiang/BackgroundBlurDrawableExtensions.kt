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
package com.sevtinge.hypercraft.libhook.utils.hookapi.blur.zhenxiang

import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import com.sevtinge.hypercraft.common.log.ModuleLog as Log
import com.sevtinge.hypercraft.common.utils.ReflectionUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass

fun BackgroundBlurDrawable.setColor(color: Int) {
    ReflectionUtils.safeInvoke(BackgroundBlurDrawable::class.java, this, "setColor", arrayOf(color))
        .onFailure { _, msg -> Log.w("BackgroundBlurDrawable", "setColor failed: $msg") }
}

fun BackgroundBlurDrawable.setBlurRadius(blurRadius: Int) {
    ReflectionUtils.safeInvoke(BackgroundBlurDrawable::class.java, this, "setBlurRadius", arrayOf(blurRadius))
        .onFailure { _, msg -> Log.w("BackgroundBlurDrawable", "setBlurRadius failed: $msg") }
}

fun BackgroundBlurDrawable.setCornerRadius(
    cornerRadiusTL: Float,
    cornerRadiusTR: Float,
    cornerRadiusBL: Float,
    cornerRadiusBR: Float
) {
    ReflectionUtils.safeInvoke(
        BackgroundBlurDrawable::class.java,
        this,
        "setCornerRadius",
        arrayOf(cornerRadiusTL, cornerRadiusTR, cornerRadiusBL, cornerRadiusBR)
    ).onFailure { _, msg -> Log.w("BackgroundBlurDrawable", "setCornerRadius failed: $msg") }
}
