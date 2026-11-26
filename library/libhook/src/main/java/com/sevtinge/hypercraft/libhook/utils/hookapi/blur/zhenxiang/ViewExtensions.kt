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

import com.sevtinge.hypercraft.common.log.ModuleLog as Log
import com.sevtinge.hypercraft.common.utils.ReflectionUtils
import android.view.View
import android.view.ViewRootImpl
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import org.lsposed.hiddenapibypass.HiddenApiBypass

fun View.createBackgroundBlurDrawable(): BackgroundBlurDrawable? {

    return ReflectionUtils.safeInvoke(View::class.java, this, "getViewRootImpl")
        .onSuccess { viewRootImpl ->
            ReflectionUtils.safeInvoke(ViewRootImpl::class.java, viewRootImpl, "createBackgroundBlurDrawable")
                .getOrDefault(null) as? BackgroundBlurDrawable
        }
        .getOrDefault(null)
}
