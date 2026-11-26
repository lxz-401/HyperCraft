/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.content.Context
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
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import java.util.Collections
import java.util.HashSet

object OneUiVolumeStyle {
    private val hookedLoaders = Collections.synchronizedSet(HashSet<ClassLoader>())
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
        PIXEL("pixel", 32, -9998140 /* #6750A4 Pixel Purple */, 60, 0x33000000),
        HYPEROS("hyperos", 22, -15168016 /* #1875F0 HyperOS Blue */, 46, 0x26000000),
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
        android.util.Log.i("HyperCraft", ">>> OneUiVolumeStyle installing safe hooks into $classLoader <<<")

        // 1. Hook VolumeColumnRes: Intercept width and radius at the root source
        runCatching {
            val volumeColumnResClass = loadClass("com.android.systemui.miui.volume.VolumeColumnRes", classLoader)

            volumeColumnResClass.declaredMethods
                .filter { it.name == "getWidth" && it.returnType == java.lang.Integer.TYPE }
                .forEach { method ->
                    method.createAfterHook { param ->
                        if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                            val widthDp = getActiveWidthDp()
                            val context = param.args.firstOrNull() as? Context
                            val density = context?.resources?.displayMetrics?.density ?: 2.75f
                            param.result = (widthDp * density).toInt()
                        }
                    }
                }

            volumeColumnResClass.declaredMethods
                .filter { it.name == "getRadius" && it.returnType == java.lang.Integer.TYPE }
                .forEach { method ->
                    method.createAfterHook { param ->
                        if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                            val radiusDp = getActiveRadiusDp()
                            val context = param.args.firstOrNull() as? Context
                            val density = context?.resources?.displayMetrics?.density ?: 2.75f
                            param.result = (radiusDp * density).toInt()
                        }
                    }
                }

            android.util.Log.i("HyperCraft", ">>> Successfully hooked VolumeColumnRes dimensions <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumeColumnRes", it)
        }

        // 2. Hook VolumeColumn: Intercept lifecycle and tint updates without layout loops
        runCatching {
            val volumeColumnClass = loadClass("com.android.systemui.miui.volume.VolumeColumn", classLoader)

            volumeColumnClass.findMethod {
                name("initColumn")
            }.createAfterHook { param ->
                applyColorsAndTints(param.thisObject)
            }

            runCatching {
                volumeColumnClass.findMethod {
                    name("setSliderTintColorList")
                    parameterTypes(Boolean::class.javaPrimitiveType!!)
                }.createAfterHook { param ->
                    applyColorsAndTints(param.thisObject)
                }
            }

            runCatching {
                volumeColumnClass.findMethod {
                    name("updateIcon")
                    parameterTypes(Boolean::class.javaPrimitiveType!!)
                }.createAfterHook { param ->
                    applyIconTint(param.thisObject)
                }
            }

            android.util.Log.i("HyperCraft", ">>> Successfully hooked VolumeColumn lifecycle <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumeColumn", it)
        }

        // 3. Hook VolumePanelViewController: Re-apply styling when panel opens
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
    }

    private fun applyColorsAndTints(column: Any?) {
        if (column == null) return
        if (isStyling.get()) return
        isStyling.set(true)
        try {
            if (!PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) return

            val preset = getActivePreset()
            val radiusDp = getActiveRadiusDp()
            val activeColor = getActiveColor()
            val bgColor = getActiveBgColor()
            val enableHaptic = PrefsBridge.getBoolean("system_ui_volume_haptic", false)

            val view = column.getObjectFieldOrNullAs<View>("view") ?: return
            val density = view.resources.displayMetrics.density
            val radiusPx = radiusDp * density

            val progressViewBg = column.getObjectFieldOrNullAs<ViewGroup>("progressViewBg")
            val progressView = column.getObjectFieldOrNullAs<View>("progressView")
            val slider = column.getObjectFieldOrNullAs<SeekBar>("slider")
            val icon = column.getObjectFieldOrNullAs<ImageView>("icon")

            // 1. Background tint & round corners
            progressViewBg?.let { bg ->
                bg.backgroundTintList = ColorStateList.valueOf(bgColor)
                bg.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
                    }
                }
                bg.clipToOutline = true
            }

            // 2. Progress fill tint & round corners
            progressView?.let { pv ->
                pv.backgroundTintList = ColorStateList.valueOf(activeColor)
                pv.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
                    }
                }
                pv.clipToOutline = true
            }

            // 3. Slider tint
            slider?.let { s ->
                s.progressTintList = ColorStateList.valueOf(activeColor)
                if (preset == VolumeStylePreset.IOS) {
                    s.thumbTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                } else {
                    s.thumbTintList = ColorStateList.valueOf(activeColor)
                }

                if (enableHaptic) {
                    val tagKey = 0x7f0a9999
                    if (s.getTag(tagKey) == null) {
                        s.setTag(tagKey, true)
                        s.setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_DOWN ||
                                event.action == MotionEvent.ACTION_MOVE
                            ) {
                                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            false
                        }
                    }
                }
            }

            // 4. Icon tint for iOS high contrast
            if (icon != null && preset == VolumeStylePreset.IOS) {
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#2C2C2E"))
            }
        } catch (t: Throwable) {
            android.util.Log.e("HyperCraft", "Error in applyColorsAndTints", t)
        } finally {
            isStyling.set(false)
        }
    }

    private fun applyIconTint(column: Any?) {
        if (column == null) return
        try {
            if (!PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) return
            val preset = getActivePreset()
            if (preset == VolumeStylePreset.IOS) {
                val icon = column.getObjectFieldOrNullAs<ImageView>("icon")
                icon?.imageTintList = ColorStateList.valueOf(Color.parseColor("#2C2C2E"))
            }
        } catch (t: Throwable) {
            android.util.Log.e("HyperCraft", "Error in applyIconTint", t)
        }
    }
}
