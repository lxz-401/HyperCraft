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
package com.sevtinge.hypercraft.libhook.rules.systemui.plugin.systemui

import android.view.View
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

object HideCollpasedFootButton {
    fun initLoaderHook(classLoader: ClassLoader) {

        loadClass(
            "com.android.systemui.miui.volume.MiuiVolumeDialogView",
            classLoader
        ).findMethod { name("updateFooterVisibility"); parameterTypes(Boolean::class.javaPrimitiveType!!) }
            .createAfterHook { param ->
            val thisObj = param.thisObject
            val mRingerModeLayout = thisObj.getObjectFieldOrNullAs<View>("mRingerModeLayout") ?: return@createAfterHook
            val mExpandButton = thisObj.getObjectFieldOrNullAs<View>("mExpandButton") ?: return@createAfterHook
            val mExpanded = runCatching { thisObj.callMethod("isExpanded") as? Boolean }.getOrNull()
                ?: return@createAfterHook
            if (mExpanded) {
                mRingerModeLayout.visibility = View.VISIBLE
                mExpandButton.visibility = View.GONE
            } else {
                mRingerModeLayout.visibility = View.GONE
                mExpandButton.visibility = if (param.args.firstOrNull() == true) View.VISIBLE else View.GONE
            }
        }

    }
}
