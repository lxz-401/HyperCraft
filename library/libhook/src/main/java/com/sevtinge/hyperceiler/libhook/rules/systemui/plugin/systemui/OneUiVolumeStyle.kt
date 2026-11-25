/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.SeekBar
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import java.util.Collections
import java.util.HashSet

object OneUiVolumeStyle {
    private val hookedLoaders = Collections.synchronizedSet(HashSet<ClassLoader>())

    enum class VolumeStylePreset(
        val id: String,
        val defaultRadiusDp: Int,
        val defaultColor: Int,
        val defaultWidthDp: Int,
        val defaultBgColor: Int = 0x33000000
    ) {
        ONE_UI("oneui", 28, -13338378 /* #3478F6 */, 52, 0x33000000),
        IOS("ios", 18, -1 /* #FFFFFF */, 46, 0x4D000000),
        PIXEL("pixel", 32, -9998140 /* #6750A4 */, 60, 0x33000000),
        HYPEROS("hyperos", 22, -15168016 /* #1875F0 */, 46, 0x26000000),
        CUSTOM("custom", 28, -13338378, 52, 0x33000000);

        companion object {
            fun fromId(id: String?): VolumeStylePreset {
                return entries.firstOrNull { it.id == id } ?: ONE_UI
            }
        }
    }

    private fun getActivePreset(): VolumeStylePreset {
        val presetId = PrefsBridge.getString("system_ui_volume_style_preset", "oneui")
        return VolumeStylePreset.fromId(presetId)
    }

    private fun getActiveRadiusDp(): Int {
        val preset = getActivePreset()
        val useCustom = preset == VolumeStylePreset.CUSTOM || PrefsBridge.getBoolean("system_ui_volume_use_custom_geometry", false)
        return if (useCustom) {
            PrefsBridge.getInt("system_ui_volume_oneui_radius", preset.defaultRadiusDp)
        } else {
            preset.defaultRadiusDp
        }
    }

    private fun getActiveWidthDp(): Int {
        val preset = getActivePreset()
        val useCustom = preset == VolumeStylePreset.CUSTOM || PrefsBridge.getBoolean("system_ui_volume_use_custom_geometry", false)
        return if (useCustom) {
            PrefsBridge.getInt("system_ui_volume_slider_width", preset.defaultWidthDp)
        } else {
            preset.defaultWidthDp
        }
    }

    private fun getActiveColor(): Int {
        val preset = getActivePreset()
        val useCustomColor = preset == VolumeStylePreset.CUSTOM || PrefsBridge.getBoolean("system_ui_volume_oneui_custom_color", false)
        return if (useCustomColor) {
            PrefsBridge.getInt("system_ui_volume_oneui_color", preset.defaultColor)
        } else {
            preset.defaultColor
        }
    }

    private fun getActiveBgColor(): Int {
        val preset = getActivePreset()
        val useCustomBg = preset == VolumeStylePreset.CUSTOM || PrefsBridge.getBoolean("system_ui_volume_custom_bg", false)
        return if (useCustomBg) {
            PrefsBridge.getInt("system_ui_volume_bg_color", preset.defaultBgColor)
        } else {
            preset.defaultBgColor
        }
    }

    fun initLoaderHook(classLoader: ClassLoader) {
        if (!hookedLoaders.add(classLoader)) {
            android.util.Log.d("HyperCraft", "OneUiVolumeStyle: already hooked for $classLoader")
            return
        }
        android.util.Log.i("HyperCraft", ">>> OneUiVolumeStyle installing hooks into $classLoader <<<")

        // 1. Hook VolumeColumn
        runCatching {
            val volumeColumnClass = loadClass("com.android.systemui.miui.volume.VolumeColumn", classLoader)

            volumeColumnClass.findMethod {
                name("initColumn")
            }.createAfterHook { param ->
                applyOneUiToColumn(param.thisObject)
            }

            volumeColumnClass.findMethod {
                name("setRadius")
                parameterTypes(Int::class.javaPrimitiveType!!)
            }.createBeforeHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val radiusDp = getActiveRadiusDp()
                    val column = param.thisObject
                    val view = column.getObjectFieldOrNullAs<View>("view")
                    val density = view?.resources?.displayMetrics?.density ?: 2.75f
                    param.args[0] = (radiusDp * density).toInt()
                }
            }

            runCatching {
                volumeColumnClass.findMethod {
                    name("setSliderTintColorList")
                    parameterTypes(Boolean::class.javaPrimitiveType!!)
                }.createAfterHook { param ->
                    applyOneUiToColumn(param.thisObject)
                }
            }

            android.util.Log.i("HyperCraft", ">>> Successfully hooked VolumeColumn <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumeColumn", it)
        }

        // 2. Hook RoundRectFrameLayout (column background container)
        runCatching {
            val roundRectClass = loadClass("com.android.systemui.miui.volume.RoundRectFrameLayout", classLoader)
            roundRectClass.findMethod {
                name("setRadius")
                parameterTypes(Int::class.javaPrimitiveType!!)
            }.createBeforeHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val radiusDp = getActiveRadiusDp()
                    val view = param.thisObject as? View
                    val density = view?.resources?.displayMetrics?.density ?: 2.75f
                    param.args[0] = (radiusDp * density).toInt()
                }
            }

            roundRectClass.findMethod {
                name("setRadius")
                parameterTypes(Int::class.javaPrimitiveType!!)
            }.createAfterHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val frame = param.thisObject as? ViewGroup ?: return@createAfterHook
                    val radiusDp = getActiveRadiusDp()
                    val density = frame.resources.displayMetrics.density
                    val radiusPx = radiusDp * density
                    frame.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                        }
                    }
                    frame.clipToOutline = true
                    frame.backgroundTintList = ColorStateList.valueOf(getActiveBgColor())
                }
            }
            android.util.Log.i("HyperCraft", ">>> Successfully hooked RoundRectFrameLayout <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook RoundRectFrameLayout", it)
        }

        // 3. Hook MiuiVolumeSeekBarProgressView (progress bar)
        runCatching {
            val progressViewClass = loadClass("com.android.systemui.miui.volume.MiuiVolumeSeekBarProgressView", classLoader)
            progressViewClass.findMethod {
                name("setRadius")
                parameterTypes(Float::class.javaPrimitiveType!!)
            }.createBeforeHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val radiusDp = getActiveRadiusDp()
                    val view = param.thisObject as? View
                    val density = view?.resources?.displayMetrics?.density ?: 2.75f
                    param.args[0] = radiusDp * density
                }
            }

            progressViewClass.findMethod {
                name("setRadius")
                parameterTypes(Float::class.javaPrimitiveType!!)
            }.createAfterHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val pv = param.thisObject as? View ?: return@createAfterHook
                    val radiusDp = getActiveRadiusDp()
                    val density = pv.resources.displayMetrics.density
                    val radiusPx = radiusDp * density
                    pv.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                        }
                    }
                    pv.clipToOutline = true
                    pv.backgroundTintList = ColorStateList.valueOf(getActiveColor())
                }
            }
            android.util.Log.i("HyperCraft", ">>> Successfully hooked MiuiVolumeSeekBarProgressView <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook MiuiVolumeSeekBarProgressView", it)
        }

        // 4. Hook VolumePanelViewController
        runCatching {
            val controllerClass = loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
            controllerClass.findMethod {
                name("showVolumePanelH")
                parameterTypes(Int::class.javaPrimitiveType!!)
            }.createAfterHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val controller = param.thisObject
                    val columns = controller.getObjectFieldOrNullAs<List<*>>("mColumns")
                    columns?.forEach { col ->
                        col?.let { applyOneUiToColumn(it) }
                    }
                    val temp = controller.getObjectFieldOrNullAs<Any>("mTempColumn")
                    temp?.let { applyOneUiToColumn(it) }
                }
            }

            controllerClass.findMethod {
                name("updateVolumeColumnH")
            }.createAfterHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    param.args.firstOrNull()?.let { applyOneUiToColumn(it) }
                }
            }
            android.util.Log.i("HyperCraft", ">>> Successfully hooked VolumePanelViewController <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumePanelViewController", it)
        }
    }

    private fun applyOneUiToColumn(column: Any) {
        runCatching {
            val enabled = PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)
            if (!enabled) return

            val preset = getActivePreset()
            val radiusDp = getActiveRadiusDp()
            val activeColor = getActiveColor()
            val widthDp = getActiveWidthDp()
            val bgColor = getActiveBgColor()
            val enableHaptic = PrefsBridge.getBoolean("system_ui_volume_haptic", false)

            val view = column.getObjectFieldOrNullAs<View>("view") ?: return
            val density = view.resources.displayMetrics.density
            val radiusPx = radiusDp * density

            // 0. Column Width
            if (widthDp > 0) {
                val widthPx = (widthDp * density).toInt()
                val lp = view.layoutParams
                if (lp != null && lp.width != widthPx) {
                    lp.width = widthPx
                    view.layoutParams = lp
                    view.requestLayout()
                }
            }

            // 1. Column radius
            runCatching {
                column.callMethod("setRadius", radiusPx.toInt())
            }

            // 2. Background container (progressViewBg: RoundRectFrameLayout)
            val progressViewBg = column.getObjectFieldOrNullAs<ViewGroup>("progressViewBg")
            progressViewBg?.let { bg ->
                runCatching { bg.callMethod("setRadius", radiusPx.toInt()) }
                bg.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
                    }
                }
                bg.clipToOutline = true
                bg.backgroundTintList = ColorStateList.valueOf(bgColor)
            }

            // 3. Progress view (progressView: MiuiVolumeSeekBarProgressView)
            val progressView = column.getObjectFieldOrNullAs<View>("progressView")
            progressView?.let { pv ->
                runCatching { pv.callMethod("setRadius", radiusPx) }
                pv.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
                    }
                }
                pv.clipToOutline = true
                pv.backgroundTintList = ColorStateList.valueOf(activeColor)
            }

            // 4. Slider (slider: SeekBar / MiuiVolumeSeekBar)
            val slider = column.getObjectFieldOrNullAs<SeekBar>("slider")
            if (slider != null) {
                slider.progressTintList = ColorStateList.valueOf(activeColor)
                slider.thumbTintList = ColorStateList.valueOf(activeColor)

                if (enableHaptic) {
                    val tagKey = 0x7f0a9999
                    if (slider.getTag(tagKey) == null) {
                        slider.setTag(tagKey, true)
                        slider.setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_DOWN ||
                                event.action == MotionEvent.ACTION_MOVE) {
                                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            false
                        }
                    }
                }
            }

            // 5. Icon
            val icon = column.getObjectFieldOrNullAs<ImageView>("icon")
            if (icon != null) {
                val iconColor = if (preset == VolumeStylePreset.IOS && !PrefsBridge.getBoolean("system_ui_volume_oneui_custom_color", false)) {
                    Color.argb(220, 50, 50, 50)
                } else {
                    Color.WHITE
                }
                icon.imageTintList = ColorStateList.valueOf(iconColor)
            }

            android.util.Log.i("HyperCraft", ">>> Style applied: preset=${preset.id}, radius=${radiusDp}dp, color=$activeColor, width=${widthDp}dp <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to applyOneUiToColumn", it)
        }
    }
}
