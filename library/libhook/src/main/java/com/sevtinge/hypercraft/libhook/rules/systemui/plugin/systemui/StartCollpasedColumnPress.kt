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

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.sevtinge.hypercraft.common.log.XposedLog
import com.sevtinge.hypercraft.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setLongField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

object StartCollpasedColumnPress {
    private const val TAG = "StartCollpasedColumnPress"

    private const val EXPAND_TOUCH_LAMBDA_PREFIX = "lambda\$processExpandTouch\$"

    fun initLoaderHook(classLoader: ClassLoader) {
        val miuiVolumeDialogView by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogView", classLoader)
        }
        val miuiVolumeDialogMotion by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogMotion", classLoader)
        }
        val miuiVolumeSeekBar by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeSeekBar", classLoader)
        }

        val active = AtomicBoolean(true)
        val handler = Handler(Looper.getMainLooper())
        data class PendingPress(val view: View, val action: Runnable)
        val pendingPresses = WeakHashMap<Any, PendingPress>()

        fun View.stopScaleAnimation() {
            animate().scaleX(1f).scaleY(1f).setDuration(300).start()
        }

        fun cancelPress(seekBar: Any) {
            pendingPresses.remove(seekBar)?.let {
                handler.removeCallbacks(it.action)
                it.view.stopScaleAnimation()
            }
        }

        BaseHook.registerHotReloadCleanup {
            runVolumeCleanupOnMain {
                active.set(false)
                pendingPresses.keys.toList().forEach { cancelPress(it) }
            }
        }

        miuiVolumeDialogView.findMethod { name("onFinishInflate") }.createAfterHook {
                    it.thisObject.getObjectFieldOrNullAs<View>("mExpandButton")?.apply {
                        alpha = 0f
                        isClickable = false
                        visibility = View.GONE
                        setOnClickListener(null)
                    }

                }
        miuiVolumeDialogView.findMethod { name("notifyAccessibilityChanged"); parameterTypes(Boolean::class.javaPrimitiveType!!) }.createAfterHook {
                    it.thisObject.getObjectFieldOrNullAs<View>("mExpandButton")?.apply {
                        isClickable = false
                        visibility = View.GONE
                        setOnClickListener(null)
                    }
                }

        hookProcessExpandTouchLambda(miuiVolumeDialogMotion)
        miuiVolumeSeekBar.findMethod { name("onTouchEvent"); parameterTypes(MotionEvent::class.java) }.createAfterHook {
                if (!active.get()) return@createAfterHook
                val seekBar = it.thisObject
                val motionEvent = it.args.firstOrNull() as? MotionEvent ?: return@createAfterHook
                if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN ||
                    motionEvent.actionMasked == MotionEvent.ACTION_UP ||
                    motionEvent.actionMasked == MotionEvent.ACTION_CANCEL) {
                    cancelPress(seekBar)
                }
                val mSeekBarOnclickListener = seekBar.getObjectFieldOrNull("mSeekBarOnclickListener")
                    ?: return@createAfterHook
                val mSeekBarAnimListener = seekBar.getObjectFieldOrNull("mSeekBarAnimListener")
                    ?: return@createAfterHook
                val volumePanelViewController = mSeekBarAnimListener.getObjectFieldOrNull($$"this$0")
                    ?: return@createAfterHook
                val mVolumeView = volumePanelViewController.getObjectFieldOrNullAs<View>("mVolumeView")
                    ?: return@createAfterHook

                runCatching { seekBar.setLongField("mCurrentMS", 0L) }
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val expanded = volumePanelViewController.getObjectFieldOrNullAs<Boolean>("mExpanded")
                            ?: return@createAfterHook
                        if (!expanded) {
                            mVolumeView.animate().scaleX(0.92f).scaleY(0.92f).setDuration(300).start()
                            val action = Runnable {
                                pendingPresses.remove(seekBar)
                                try {
                                    val moveY = seekBar.getObjectFieldOrNullAs<Float>("mMoveY")
                                    if (active.get() && moveY != null && abs(moveY) < 10f) {
                                        mVolumeView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        runCatching { mSeekBarOnclickListener.callMethod("onClick") }
                                            .onFailure { error -> XposedLog.e(TAG, "Can't expand volume panel: $error") }
                                    }
                                } finally {
                                    mVolumeView.stopScaleAnimation()
                                }
                            }
                            pendingPresses[seekBar] = PendingPress(mVolumeView, action)
                            if (!handler.postDelayed(action, 300)) cancelPress(seekBar)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mVolumeView.stopScaleAnimation()
                }
            }

    }

    private fun hookProcessExpandTouchLambda(hostClass: Class<*>?) {
        // 按结构特征定位“展开/收起触摸”那个合成 lambda 并挂前置 hook。
        // 编号在不同固件上会漂：OS3 = lambda$processExpandTouch$1，
        // OS4（4.0.0.44 / 4.0.5.0）= lambda$processExpandTouch$2，
        // 三套固件里“前缀 + 无参 + void”均为唯一命中。
        // 之所以不能写死 EXPAND_TOUCH_LAMBDA_PREFIX 里的编号：findMethod 落空会抛异常，
        // 被 loadClassLoaders 的 runCatching 整个吞掉，连带本 loader 里后面的 hook 一起失效。
        if (hostClass == null) {
            return
        }
        var hooked = 0
        for (method in hostClass.declaredMethods) {
            if (method.name.startsWith(EXPAND_TOUCH_LAMBDA_PREFIX)
                && method.parameterTypes.isEmpty()
                && method.returnType == Void.TYPE
            ) {
                BaseHook.hookMethod(method, object : IMethodHook {
                    override fun before(param: HookParam) {
                        val owner = param.thisObject ?: return
                        BaseHook.setBooleanField(owner, "mIsExpandButton", true)
                    }
                })
                hooked++
            }
        }
        if (hooked == 0) {
            XposedLog.d(TAG, "can't find processExpandTouch lambda in " + hostClass.name)
        }
    }
}
