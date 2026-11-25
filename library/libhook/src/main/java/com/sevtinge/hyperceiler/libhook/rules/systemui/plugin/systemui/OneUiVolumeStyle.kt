/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.content.res.ColorStateList
import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.SeekBar
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import java.util.concurrent.atomic.AtomicBoolean

object OneUiVolumeStyle : BaseHook() {
    private val isHooked = AtomicBoolean(false)

    override fun init() {
        android.util.Log.e("HyperCraft", ">>> OneUiVolumeStyle BaseHook.init() called <<<")
        val classLoader = this::class.java.classLoader ?: return
        hookClasses(classLoader)
    }

    fun initLoaderHook(classLoader: ClassLoader) {
        android.util.Log.e("HyperCraft", ">>> OneUiVolumeStyle initLoaderHook called with $classLoader <<<")
        hookClasses(classLoader)
    }

    private fun hookClasses(classLoader: ClassLoader) {
        if (!isHooked.compareAndSet(false, true)) return
        android.util.Log.e("HyperCraft", ">>> OneUiVolumeStyle installing hooks into $classLoader <<<")

        // 1. Hook VolumeColumn
        runCatching {
            val volumeColumnClass = loadClass("com.android.systemui.miui.volume.VolumeColumn", classLoader)

            // Hook initColumn
            volumeColumnClass.findMethod {
                name("initColumn")
            }.createAfterHook { param ->
                applyOneUiToColumn(param.thisObject)
            }

            // Hook setRadius(int) on VolumeColumn
            volumeColumnClass.findMethod {
                name("setRadius")
                parameterTypes(Int::class.javaPrimitiveType!!)
            }.createBeforeHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
                    val column = param.thisObject
                    val view = column.getObjectFieldOrNullAs<View>("view")
                    val density = view?.resources?.displayMetrics?.density ?: 2.75f
                    param.args[0] = (radiusDp * density).toInt()
                }
            }

            // Hook setSliderTintColorList
            runCatching {
                volumeColumnClass.findMethod {
                    name("setSliderTintColorList")
                    parameterTypes(Boolean::class.javaPrimitiveType!!)
                }.createAfterHook { param ->
                    applyOneUiToColumn(param.thisObject)
                }
            }

            android.util.Log.e("HyperCraft", ">>> Successfully hooked VolumeColumn <<<")
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
                    val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
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
                    val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
                    val density = frame.resources.displayMetrics.density
                    val radiusPx = radiusDp * density
                    frame.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                        }
                    }
                    frame.clipToOutline = true
                }
            }
            android.util.Log.e("HyperCraft", ">>> Successfully hooked RoundRectFrameLayout <<<")
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
                    val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
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
                    val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
                    val density = pv.resources.displayMetrics.density
                    val radiusPx = radiusDp * density
                    pv.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                        }
                    }
                    pv.clipToOutline = true
                }
            }

            // Hook onMeasure to set color and outline
            progressViewClass.findMethod {
                name("onMeasure")
                parameterTypes(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
            }.createAfterHook { param ->
                if (PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)) {
                    val view = param.thisObject as? View ?: return@createAfterHook
                    val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
                    val density = view.resources.displayMetrics.density
                    val radiusPx = radiusDp * density
                    view.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(v: View, outline: Outline) {
                            outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
                        }
                    }
                    view.clipToOutline = true

                    val useCustomColor = PrefsBridge.getBoolean("system_ui_volume_oneui_custom_color", false)
                    if (useCustomColor) {
                        val customColor = PrefsBridge.getInt("system_ui_volume_oneui_color", -13338378)
                        view.backgroundTintList = ColorStateList.valueOf(customColor)
                    }
                }
            }
            android.util.Log.e("HyperCraft", ">>> Successfully hooked MiuiVolumeSeekBarProgressView <<<")
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
            android.util.Log.e("HyperCraft", ">>> Successfully hooked VolumePanelViewController <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to hook VolumePanelViewController", it)
        }
    }

    private fun applyOneUiToColumn(column: Any) {
        runCatching {
            val enabled = PrefsBridge.getBoolean("system_ui_volume_oneui_style", false)
            if (!enabled) return

            val radiusDp = PrefsBridge.getInt("system_ui_volume_oneui_radius", 28)
            val useCustomColor = PrefsBridge.getBoolean("system_ui_volume_oneui_custom_color", false)
            val customColor = PrefsBridge.getInt("system_ui_volume_oneui_color", -13338378) // #3478F6

            val view = column.getObjectFieldOrNullAs<View>("view") ?: return
            val density = view.resources.displayMetrics.density
            val radiusPx = radiusDp * density

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
                if (useCustomColor) {
                    pv.backgroundTintList = ColorStateList.valueOf(customColor)
                }
            }

            // 4. Slider (slider: SeekBar / MiuiVolumeSeekBar)
            val slider = column.getObjectFieldOrNullAs<SeekBar>("slider")
            if (useCustomColor && slider != null) {
                slider.progressTintList = ColorStateList.valueOf(customColor)
                slider.thumbTintList = ColorStateList.valueOf(customColor)
            }

            // 5. Icon
            val icon = column.getObjectFieldOrNullAs<ImageView>("icon")
            if (useCustomColor && icon != null) {
                icon.imageTintList = ColorStateList.valueOf(customColor)
            }

            android.util.Log.e("HyperCraft", ">>> One UI style applied to column: radius=${radiusDp}dp, customColor=$useCustomColor <<<")
        }.onFailure {
            android.util.Log.e("HyperCraft", "Failed to applyOneUiToColumn", it)
        }
    }
}
