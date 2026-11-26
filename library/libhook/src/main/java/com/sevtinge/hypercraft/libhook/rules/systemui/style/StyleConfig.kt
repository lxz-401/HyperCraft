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
 * Snapshot of the style-relevant user settings, read through [PrefsBridge].
 *
 * Hooks that only need to *react* to a style state (e.g. preset validation,
 * conflict detection) can take a snapshot instead of re-reading every key on
 * their own. The data class is immutable and cheap to compare.
 */
data class StyleConfig(
    // Status bar clock
    val clockEnabled: Boolean,
    val clockStyle: Int,
    val clockColorMode: Int,
    val clockFormatStatus: String,
    // Battery percentage text
    val batteryPercentEnabled: Boolean,
    val batteryPercentAtRight: Boolean,
    val batteryPercentShowSign: Boolean,
    // Battery detail indicator
    val batteryDetailEnabled: Boolean,
    // Volume panel style
    val volumeOneUiEnabled: Boolean,
    val volumePreset: String,
    // Control center layout
    val oldControlCenterEnabled: Boolean,
) {
    companion object {
        private const val KEY_CLOCK_ENABLE = "system_ui_statusbar_clock_all_status_enable"
        private const val KEY_CLOCK_STYLE = "system_ui_statusbar_clock_style"
        private const val KEY_CLOCK_COLOR = "system_ui_statusbar_clock_color_mode"
        private const val KEY_CLOCK_FORMAT_S = "system_ui_statusbar_clock_editor_s"
        private const val KEY_BATTERY_PERCENT_ENABLE = "system_ui_statusbar_battery_percent_enable"
        private const val KEY_BATTERY_PERCENT_RIGHT = "system_ui_statusbar_battery_percent_at_right"
        private const val KEY_BATTERY_PERCENT_SIGN = "system_ui_statusbar_battery_percent_show_sign"
        private const val KEY_BATTERY_DETAIL_ENABLE = "system_ui_statusbar_battery_detail_enable"
        private const val KEY_VOLUME_ONEUI = "system_ui_volume_oneui_style"
        private const val KEY_VOLUME_PRESET = "system_ui_volume_style_preset"
        private const val KEY_OLD_CC = "system_control_center_old_enable"

        /**
         * Reads the current settings into an immutable snapshot.
         * Safe to call from the app process or from hooked processes.
         */
        fun fromPrefs(): StyleConfig = StyleConfig(
            clockEnabled = PrefsBridge.getBoolean(KEY_CLOCK_ENABLE),
            clockStyle = PrefsBridge.getStringAsInt(KEY_CLOCK_STYLE, 0),
            clockColorMode = PrefsBridge.getStringAsInt(KEY_CLOCK_COLOR, 0),
            clockFormatStatus = PrefsBridge.getString(KEY_CLOCK_FORMAT_S, "HH:mm"),
            batteryPercentEnabled = PrefsBridge.getBoolean(KEY_BATTERY_PERCENT_ENABLE),
            batteryPercentAtRight = PrefsBridge.getBoolean(KEY_BATTERY_PERCENT_RIGHT),
            batteryPercentShowSign = PrefsBridge.getBoolean(KEY_BATTERY_PERCENT_SIGN, true),
            batteryDetailEnabled = PrefsBridge.getBoolean(KEY_BATTERY_DETAIL_ENABLE),
            volumeOneUiEnabled = PrefsBridge.getBoolean(KEY_VOLUME_ONEUI),
            volumePreset = PrefsBridge.getString(KEY_VOLUME_PRESET, "oneui"),
            oldControlCenterEnabled = PrefsBridge.getBoolean(KEY_OLD_CC),
        )

        /** True when two snapshots differ in a way that would restart SystemUI. */
        fun differs(other: StyleConfig): Boolean = this != other
    }
}
