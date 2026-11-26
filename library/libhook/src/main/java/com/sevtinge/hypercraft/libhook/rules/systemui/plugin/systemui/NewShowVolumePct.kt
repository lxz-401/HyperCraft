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

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hypercraft.libhook.utils.hookapi.systemui.mSupportSV
import com.sevtinge.hypercraft.libhook.utils.hookapi.tool.VolumeProgress
import com.sevtinge.hypercraft.libhook.utils.hookapi.tool.AppsTool.initPct
import com.sevtinge.hypercraft.libhook.utils.hookapi.tool.AppsTool.mPct
import com.sevtinge.hypercraft.libhook.utils.hookapi.tool.AppsTool.removePct
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getIntField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import java.util.WeakHashMap

object NewShowVolumePct {

    private val streamCache = WeakHashMap<Any, Int>()
    @JvmStatic
    fun initLoader(classLoader: ClassLoader) {
        val volumePanelViewControllerClazz by lazy {
            loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
        }
        val volumePanelViewControllerListener by lazy {
            if (isMoreHyperOSVersion(3f)) {
                loadClass($$"miui.systemui.controlcenter.panel.main.volume.VolumeSliderController$seekBarListener$1", classLoader)
            } else {
                loadClass($$"com.android.systemui.miui.volume.VolumePanelViewController$VolumeSeekBarChangeListener", classLoader)
            }
        }

        volumePanelViewControllerClazz.findMethod { name("showVolumePanelH") }.createAfterHook {
                val mVolumeView = it.thisObject.getObjectFieldOrNullAs<View>("mVolumeView")
                    ?: return@createAfterHook
                val windowView = mVolumeView.parent as? ViewGroup ?: return@createAfterHook
                initPct(windowView, 3)
            }

        mVolumeDisable(volumePanelViewControllerClazz)
        onProgressChanged(volumePanelViewControllerListener, mSupportSV)
    }

    private fun mVolumeDisable(clazz: Class<*>) {
        clazz.findMethod { name("dismissH") }.createAfterHook {
                removePct(mPct)
            }
    }

    private fun getOrCacheStream(volumeColumn: Any): Int {
        streamCache[volumeColumn]?.let { return it }
        val stream = try {
            volumeColumn.getObjectFieldOrNullAs<Int>("stream")
                ?: (volumeColumn.callMethod("getStream") as? Int)
                ?: -1
        } catch (_: Throwable) {
            -1
        }
        streamCache[volumeColumn] = stream
        return stream
    }

    @SuppressLint("SetTextI18n")
    private fun onProgressChanged(clazz: Class<*>, mSupportSV: Boolean) {
        clazz.findMethod { name("onProgressChanged") }.createAfterHook {
                var mTag = 0
                var currentLevel: Int
                val seekBar = it.args[0] as SeekBar
                val arg1 = it.args[1] as Int
                val arg2 = it.args[2] as Boolean
                val isObj: Any = if (isMoreHyperOSVersion(3f)) {
                    it.thisObject.getObjectFieldOrNull("this$0")
                } else {
                    it.thisObject
                } ?: return@createAfterHook

                if (mPct != null && mPct.tag != null) {
                    mTag = mPct.tag as? Int ?: 0
                }

                if (mTag != 3 || mPct == null) return@createAfterHook

                val mColumn =
                    isObj.getObjectFieldOrNull("mColumn") ?: return@createAfterHook
                val ss = mColumn.getObjectFieldOrNull("ss") ?: return@createAfterHook

                if (getOrCacheStream(mColumn) == 10) return@createAfterHook

                currentLevel = if (arg2) {
                    arg1
                } else {
                    val anim = mColumn.getObjectFieldOrNullAs<ObjectAnimator>("anim")
                    if (anim == null || !anim.isRunning) return@createAfterHook
                    mColumn.getIntField("animTargetProgress")
                }

                mPct.visibility = View.VISIBLE

                val levelMin = ss.getIntField("levelMin")
                if (levelMin > 0 && currentLevel < levelMin * 1000) {
                    currentLevel = levelMin * 1000
                }

                val percentage = VolumeProgress.volumePercentage(currentLevel, seekBar.max)
                mPct.text = if (percentage == 100 && mSupportSV) "200%" else "$percentage%"
            }
    }
}
