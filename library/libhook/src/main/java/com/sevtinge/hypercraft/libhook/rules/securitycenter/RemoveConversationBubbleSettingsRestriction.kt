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
package com.sevtinge.hypercraft.libhook.rules.securitycenter

import android.annotation.SuppressLint
import android.content.Context
import android.util.ArrayMap
import com.sevtinge.hypercraft.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import com.sevtinge.hypercraft.common.utils.ReflectionUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass

class RemoveConversationBubbleSettingsRestriction : BaseHook() {
    @SuppressLint("PrivateApi")
    override fun init() {
        loadClass("com.miui.bubbles.settings.BubblesSettings").findMethod { name("getDefaultBubbles") }.createHook {
                before { param ->
                    val classBubbleApp = loadClass("com.miui.bubbles.settings.BubbleApp")
                    val arrayMap = ArrayMap<String, Any>()
                    val mContext =
                        param.thisObject.getObjectField("mContext") as Context
                    val mCurrentUserId =
                        param.thisObject.getObjectField("mCurrentUserId") as Int
                    val freeformSuggestionList = ReflectionUtils.safeInvokeStatic(
                        Class.forName("android.util.MiuiMultiWindowUtils"),
                        "getFreeformSuggestionList",
                        arrayOf(mContext)
                    ).getOrDefault(emptyList()) as List<*>
                    if (freeformSuggestionList.isNotEmpty()) {
                        for (str in freeformSuggestionList) {
                            val bubbleApp = classBubbleApp.getConstructor(
                                String::class.java, Int::class.java
                            ).newInstance(str, mCurrentUserId)
                            classBubbleApp.getMethod("setChecked", Boolean::class.java)
                                .invoke(bubbleApp, true)
                            arrayMap[str as String] = bubbleApp
                        }
                    }
                    param.result = arrayMap
                }
            }
    }
}
