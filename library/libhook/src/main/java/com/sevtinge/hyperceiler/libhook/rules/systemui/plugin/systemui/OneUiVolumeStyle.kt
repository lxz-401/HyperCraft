/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Outline
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.SeekBar
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import java.util.WeakHashMap
import kotlin.math.roundToInt

object OneUiVolumeStyle {
    @Volatile private var active = false
    private val originalStyles = WeakHashMap<View, OriginalStyle>()
    private val iconTints = WeakHashMap<ImageView, ColorStateList?>()
    private val lastHapticLevel = WeakHashMap<SeekBar, Int>()

    private class RoundedOutline(var radius: Float) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val effectiveRadius = radius.coerceAtMost(minOf(view.width, view.height) / 2f)
            outline.setRoundRect(0, 0, view.width, view.height, effectiveRadius)
        }
    }

    private class OriginalStyle(view: View) {
        private val backgroundTint = view.backgroundTintList
        private val outline = view.outlineProvider
        private val clip = view.clipToOutline
        private var progressTint = (view as? SeekBar)?.progressTintList
        private var thumbTint = (view as? SeekBar)?.thumbTintList
        val roundedOutline = RoundedOutline(0f)

        fun captureSliderTint(slider: SeekBar) {
            progressTint = slider.progressTintList
            thumbTint = slider.thumbTintList
        }

        fun restore(view: View) {
            if (view is SeekBar) {
                view.progressTintList = progressTint
                view.thumbTintList = thumbTint
            } else {
                view.backgroundTintList = backgroundTint
                view.outlineProvider = outline
                view.clipToOutline = clip
            }
        }
    }
    private val isStyling = ThreadLocal.withInitial { false }

    enum class VolumeStylePreset(
        val id: String,
        val defaultRadiusDp: Int,
        val defaultColor: Int,
        val defaultWidthDp: Int,
        val defaultBgColor: Int = 0x33000000
    ) {
        ONE_UI("oneui", 28, -13338378 /* #3478F6 Samsung Blue */, 52, 0x33000000),
        IOS("ios", 18, -1 /* #FFFFFF iOS White */, 46, 0x4D000000),
        PIXEL("pixel", 32, 0xFF6750A4.toInt() /* Pixel Purple */, 60, 0x33000000),
        HYPEROS("hyperos", 22, 0xFF1875F0.toInt() /* HyperOS Blue */, 46, 0x26000000),
        CUSTOM("custom", 28, -13338378, 52, 0x33000000);

        companion object {
            fun fromId(id: String?): VolumeStylePreset {
                return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ONE_UI
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
            PrefsBridge.getInt("system_ui_volume_oneui_radius", preset.defaultRadiusDp).coerceIn(8, 50)
        } else {
            preset.defaultRadiusDp
        }
    }

    private fun getActiveWidthDp(): Int {
        val preset = getActivePreset()
        val useCustom = preset == VolumeStylePreset.CUSTOM || PrefsBridge.getBoolean("system_ui_volume_use_custom_geometry", false)
        return if (useCustom) {
            PrefsBridge.getInt("system_ui_volume_slider_width", preset.defaultWidthDp).coerceIn(36, 85)
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
        active = true
        BaseHook.registerHotReloadCleanup {
            runVolumeCleanupOnMain {
                active = false
                originalStyles.forEach { (view, style) -> style.restore(view) }
                iconTints.forEach { (icon, tint) -> icon.imageTintList = tint }
                originalStyles.clear()
                iconTints.clear()
                lastHapticLevel.clear()
            }
        }

        // 1. Hook VolumeColumnRes: Intercept width and radius at the root source
        runCatching {
            val volumeColumnResClass = loadClass("com.android.systemui.miui.volume.VolumeColumnRes", classLoader)

            volumeColumnResClass.declaredMethods
                .filter { it.name == "getWidth" && it.returnType == java.lang.Integer.TYPE }
                .forEach { method ->
                    method.createAfterHook { param ->
                        if (active && PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                            val widthDp = getActiveWidthDp()
                            val density = densityFor(param.args)
                            param.result = (widthDp * density).roundToInt()
                        }
                    }
                }

            volumeColumnResClass.declaredMethods
                .filter { it.name == "getRadius" && it.returnType == java.lang.Integer.TYPE }
                .forEach { method ->
                    method.createAfterHook { param ->
                        if (active && PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                            val radiusDp = getActiveRadiusDp()
                            val density = densityFor(param.args)
                            param.result = (radiusDp * density).roundToInt()
                        }
                    }
                }

            android.util.Log.i("HyperCraft", ">>> Checked VolumeColumnRes dimensions <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumeColumnRes", it)
        }

        // 2. Hook VolumeColumn: Intercept lifecycle and tint updates without layout loops
        runCatching {
            val volumeColumnClass = loadClass("com.android.systemui.miui.volume.VolumeColumn", classLoader)

            volumeColumnClass.declaredMethods.filter { it.name == "initColumn" }.forEach { method ->
                method.createAfterHook { param -> applyColorsAndTints(param.thisObject) }
            }

            runCatching {
                volumeColumnClass.findMethod {
                    name("setSliderTintColorList")
                    parameterTypes(Boolean::class.javaPrimitiveType!!)
                }.createAfterHook { param ->
                    if (!active) return@createAfterHook
                    val slider = param.thisObject.getObjectFieldOrNullAs<SeekBar>("slider")
                    if (slider != null) originalStyles[slider]?.captureSliderTint(slider)
                    applyColorsAndTints(param.thisObject)
                }
            }

            runCatching {
                volumeColumnClass.findMethod {
                    name("updateIcon")
                    parameterTypes(Boolean::class.javaPrimitiveType!!)
                }.createAfterHook { param ->
                    applyIconTint(param.thisObject, true)
                }
            }

            android.util.Log.i("HyperCraft", ">>> Successfully hooked VolumeColumn lifecycle <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumeColumn", it)
        }

        // 3. Hook VolumePanelViewController: Re-apply styling when panel opens
        runCatching {
            val controllerClass = loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)

            controllerClass.declaredMethods.filter { it.name == "showVolumePanelH" }.forEach { method ->
                method.createAfterHook { param ->
                    val controller = param.thisObject
                    val columns = controller.getObjectFieldOrNullAs<List<*>>("mColumns")
                    columns?.forEach { col ->
                        col?.let { applyColorsAndTints(it) }
                    }
                    val temp = controller.getObjectFieldOrNullAs<Any>("mTempColumn")
                    temp?.let { applyColorsAndTints(it) }
                }
            }

            android.util.Log.i("HyperCraft", ">>> Successfully hooked VolumePanelViewController <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumePanelViewController", it)
        }
        // Observe the existing touch handler instead of replacing the plugin's listener.
        runCatching {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeSeekBar", classLoader)
                .findMethod { name("onTouchEvent"); parameterTypes(MotionEvent::class.java) }
                .createAfterHook { param ->
                    val slider = param.thisObject as? SeekBar ?: return@createAfterHook
                    val event = param.args.firstOrNull() as? MotionEvent ?: return@createAfterHook
                    if (!active || !PrefsBridge.getBoolean("system_ui_volume_oneui_style", false) ||
                        !PrefsBridge.getBoolean("system_ui_volume_haptic", false)) {
                        lastHapticLevel.remove(slider)
                        return@createAfterHook
                    }
                    val level = if (slider.max >= 1000) slider.progress / 1000 else slider.progress
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> lastHapticLevel[slider] = level
                        MotionEvent.ACTION_MOVE -> {
                            val previous = lastHapticLevel.put(slider, level)
                            if (previous != null && previous != level) {
                                slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> lastHapticLevel.remove(slider)
                    }
                }
        }
    }

    private fun densityFor(args: Array<out Any?>): Float {
        val context = args.firstOrNull { it is Context } as? Context
            ?: EzXposed.appContextOrNull
        return (context?.resources ?: Resources.getSystem()).displayMetrics.density
    }

    private fun restoreView(view: View?) {
        if (view != null) originalStyles.remove(view)?.restore(view)
    }

    private fun applyColorsAndTints(column: Any?) {
        if (!active || column == null || isStyling.get() == true) return
        isStyling.set(true)
        try {
            val progressViewBg = column.getObjectFieldOrNullAs<View>("progressViewBg")
            val progressView = column.getObjectFieldOrNullAs<View>("progressView")
            val slider = column.getObjectFieldOrNullAs<SeekBar>("slider")
            if (!PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                restoreView(progressViewBg)
                restoreView(progressView)
                restoreView(slider)
                applyIconTint(column)
                return
            }

            val view = column.getObjectFieldOrNullAs<View>("view") ?: return
            val radiusPx = getActiveRadiusDp() * view.resources.displayMetrics.density
            val color = ColorStateList.valueOf(getActiveColor())
            for ((target, tint) in listOf(progressViewBg to ColorStateList.valueOf(getActiveBgColor()), progressView to color)) {
                if (target == null) continue
                val style = originalStyles.getOrPut(target) { OriginalStyle(target) }
                style.roundedOutline.radius = radiusPx
                target.backgroundTintList = tint
                target.outlineProvider = style.roundedOutline
                target.clipToOutline = true
                target.invalidateOutline()
            }
            slider?.let {
                originalStyles.getOrPut(it) { OriginalStyle(it) }
                it.progressTintList = color
                it.thumbTintList = if (getActivePreset() == VolumeStylePreset.IOS) {
                    ColorStateList.valueOf(Color.TRANSPARENT)
                } else color
            }
            applyIconTint(column)
        } catch (t: Throwable) {
            android.util.Log.e("HyperCraft", "Error in applyColorsAndTints", t)
        } finally {
            isStyling.set(false)
        }
    }

    private fun applyIconTint(column: Any?, fromOriginalUpdate: Boolean = false) {
        if (!active || column == null) return
        val icon = column.getObjectFieldOrNullAs<ImageView>("icon") ?: return
        if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false) && getActivePreset() == VolumeStylePreset.IOS) {
            // updateIcon may have changed the OEM tint for the current mute state.
            if (fromOriginalUpdate || !iconTints.containsKey(icon)) iconTints[icon] = icon.imageTintList
            icon.imageTintList = ColorStateList.valueOf(0xFF2C2C2E.toInt())
        } else if (iconTints.containsKey(icon)) {
            val original = iconTints.remove(icon)
            if (!fromOriginalUpdate) icon.imageTintList = original
        }
    }
}
