/*
 * This file is part of HyperCraft.
 *
 * HyperCraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCraft Contributions
 */
package com.sevtinge.hypercraft.libhook.rules.systemui.style

import com.sevtinge.hypercraft.common.utils.PrefsBridge

/**
 * A built-in style preset: a named bundle of style settings that can be
 * applied in one step via [PrefsBridge].
 *
 * Values are written with [PrefsBridge.put], which on the app process side
 * pushes the changes to the hooked SystemUI processes as well.
 */
data class StylePreset(
    val id: String,
    val labelKey: String,
    val values: Map<String, Any>
) {
    /**
     * Applies every key/value pair of this preset. Individual failures are
     * swallowed: applying a preset must never crash the settings UI.
     */
    fun apply() {
        values.forEach { (key, value) ->
            runCatching { PrefsBridge.put(key, value) }
        }
    }

    companion object {
        /** "Custom" acts as a no-op preset: applying it changes nothing. */
        @JvmField
        val CUSTOM = StylePreset("custom", "style_preset_custom", emptyMap())

        @JvmField
        val MATERIAL = StylePreset(
            "material",
            "style_preset_material",
            mapOf(
                "system_ui_statusbar_clock_all_status_enable" to true,
                "system_ui_statusbar_clock_style" to 0,
                "system_ui_statusbar_clock_color_mode" to 0,
                "system_ui_statusbar_clock_editor_s" to "HH:mm",
                "system_ui_statusbar_battery_percent_enable" to true,
                "system_ui_statusbar_battery_percent_at_right" to true,
                "system_ui_statusbar_battery_percent_show_sign" to true,
                "system_ui_statusbar_battery_detail_enable" to false,
                "system_ui_volume_oneui_style" to false,
                "system_control_center_old_enable" to false,
            )
        )

        val ONE_UI_CAPSULE = StylePreset(
            "oneui",
            "style_preset_oneui",
            mapOf(
                "system_ui_statusbar_clock_all_status_enable" to true,
                "system_ui_statusbar_clock_style" to 0,
                "system_ui_statusbar_clock_color_mode" to 0,
                "system_ui_statusbar_clock_editor_s" to "HH:mm",
                "system_ui_statusbar_battery_percent_enable" to true,
                "system_ui_statusbar_battery_percent_at_right" to true,
                "system_ui_statusbar_battery_percent_show_sign" to true,
                "system_ui_statusbar_battery_detail_enable" to false,
                "system_ui_volume_oneui_style" to true,
                "system_ui_volume_style_preset" to "oneui",
                "system_control_center_old_enable" to false,
            )
        )

        val BOLD_MINIMAL = StylePreset(
            "minimal",
            "style_preset_minimal",
            mapOf(
                "system_ui_statusbar_clock_all_status_enable" to true,
                "system_ui_statusbar_clock_style" to 2,
                "system_ui_statusbar_clock_color_mode" to 0,
                "system_ui_statusbar_clock_editor_s" to "HH:mm",
                "system_ui_statusbar_clock_editor_b" to "HH:mm",
                "system_ui_statusbar_battery_percent_enable" to true,
                "system_ui_statusbar_battery_percent_at_right" to true,
                "system_ui_statusbar_battery_percent_show_sign" to true,
                "system_ui_statusbar_battery_detail_enable" to false,
                "system_ui_volume_oneui_style" to false,
                "system_control_center_old_enable" to false,
            )
        )

        /** The "Custom" entry is last: it means "stop applying built-ins". */
        @JvmField
        val BUILT_IN = listOf(MATERIAL, ONE_UI_CAPSULE, BOLD_MINIMAL, CUSTOM)

        @JvmStatic
        fun byId(id: String): StylePreset? = BUILT_IN.firstOrNull { it.id == id } ?: CUSTOM

        /**
         * Keys covered by export/import. The set is intentionally small and
         * stable: preset files stay readable across module versions.
         */
        @JvmField
        val EXPORT_KEYS = listOf(
            "system_ui_statusbar_clock_all_status_enable",
            "system_ui_statusbar_clock_style",
            "system_ui_statusbar_clock_color_mode",
            "system_ui_statusbar_clock_editor_s",
            "system_ui_statusbar_clock_editor_b",
            "system_ui_statusbar_battery_percent_enable",
            "system_ui_statusbar_battery_percent_at_right",
            "system_ui_statusbar_battery_percent_show_sign",
            "system_ui_statusbar_battery_detail_enable",
            "system_ui_volume_oneui_style",
            "system_ui_volume_style_preset",
            "system_control_center_old_enable"
        )
    }
}
