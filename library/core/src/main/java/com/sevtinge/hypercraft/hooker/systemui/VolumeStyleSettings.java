/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hypercraft.hooker.systemui;

import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

import java.util.Locale;

import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.core.R;
import com.sevtinge.hypercraft.dashboard.DashboardFragment;
import com.sevtinge.hypercraft.hooker.systemui.widget.VolumeStylePreviewView;
import com.sevtinge.hypercraft.prefs.LayoutPreference;

public class VolumeStyleSettings extends DashboardFragment
    implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private VolumeStylePreviewView mPreviewView;
    private SharedPreferences mObservedPreferences;
    private boolean mRefreshing;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_volume_style;
    }

    @Override
    public void initPrefs() {
        LayoutPreference previewPref = findPreference("prefs_key_volume_style_preview");
        if (previewPref != null) {
            mPreviewView = previewPref.findViewById(R.id.volume_preview_view);
        }

        bindPrefListener("prefs_key_system_ui_volume_style_preset");
        bindPrefListener("prefs_key_system_ui_volume_oneui_radius");
        bindPrefListener("prefs_key_system_ui_volume_slider_width");
        bindPrefListener("prefs_key_system_ui_volume_oneui_color");
        bindPrefListener("prefs_key_system_ui_volume_bg_color");
        bindPrefListener("prefs_key_system_ui_volume_use_custom_geometry");
        bindPrefListener("prefs_key_system_ui_volume_oneui_custom_color");
        bindPrefListener("prefs_key_system_ui_volume_custom_bg");
        bindPrefListener("prefs_key_system_ui_volume_oneui_style");

        refreshPreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
        mObservedPreferences = PrefsBridge.getSharedPreferences();
        if (mObservedPreferences != null) {
            mObservedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPreferences();
    }

    @Override
    public void onStop() {
        if (mObservedPreferences != null) {
            mObservedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            mObservedPreferences = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mPreviewView = null;
        super.onDestroyView();
    }

    private void scheduleRefresh() {
        View view = getView();
        if (view != null) {
            view.post(() -> {
                if (getView() == view && isAdded()) refreshPreferences();
            });
        }
    }

    private void refreshPreferences() {
        if (mRefreshing) return;
        mRefreshing = true;
        try {
            syncSwitch("system_ui_volume_oneui_style");
            syncSwitch("system_ui_volume_use_custom_geometry");
            syncSwitch("system_ui_volume_oneui_custom_color");
            syncSwitch("system_ui_volume_custom_bg");
            syncSwitch("system_ui_volume_haptic");
            String preset = PrefsBridge.getString("system_ui_volume_style_preset", "oneui");
            DropDownPreference presetPreference = findPreference("prefs_key_system_ui_volume_style_preset");
            if (presetPreference != null && !preset.equals(presetPreference.getValue())) {
                boolean persistent = presetPreference.isPersistent();
                presetPreference.setPersistent(false);
                presetPreference.setValue(preset);
                presetPreference.setPersistent(persistent);
            }
            syncNumber("system_ui_volume_oneui_radius", 28);
            syncNumber("system_ui_volume_slider_width", 52);
            syncNumber("system_ui_volume_oneui_color", 0xFF3478F6);
            syncNumber("system_ui_volume_bg_color", 0x33000000);
            boolean enabled = PrefsBridge.getBoolean("system_ui_volume_oneui_style", false);
            boolean custom = "custom".equalsIgnoreCase(preset);
            setControlEnabled("system_ui_volume_oneui_radius", enabled && (custom || PrefsBridge.getBoolean("system_ui_volume_use_custom_geometry", false)));
            setControlEnabled("system_ui_volume_slider_width", enabled && (custom || PrefsBridge.getBoolean("system_ui_volume_use_custom_geometry", false)));
            setControlEnabled("system_ui_volume_oneui_color", enabled && (custom || PrefsBridge.getBoolean("system_ui_volume_oneui_custom_color", false)));
            setControlEnabled("system_ui_volume_bg_color", enabled && (custom || PrefsBridge.getBoolean("system_ui_volume_custom_bg", false)));
            updatePreview();
        } finally {
            mRefreshing = false;
        }
    }

    private void syncSwitch(String key) {
        TwoStatePreference preference = findPreference("prefs_key_" + key);
        if (preference == null) return;
        boolean persistent = preference.isPersistent();
        preference.setPersistent(false);
        preference.setChecked(PrefsBridge.getBoolean(key, false));
        preference.setPersistent(persistent);
    }

    private void syncNumber(String key, int defaultValue) {
        Preference preference = findPreference("prefs_key_" + key);
        if (preference == null) return;
        int value = PrefsBridge.getInt(key, defaultValue);
        boolean persistent = preference.isPersistent();
        preference.setPersistent(false);
        if (preference instanceof SeekBarPreferenceCompat seekBar && seekBar.getCurrentValue() != value) {
            seekBar.setCurrentValue(value);
        } else if (preference instanceof ColorPickerPreference picker && picker.getColor() != value) {
            picker.setColor(value);
        }
        preference.setPersistent(persistent);
    }

    private void setControlEnabled(String key, boolean enabled) {
        Preference preference = findPreference("prefs_key_" + key);
        if (preference != null) preference.setEnabled(enabled);
    }

    private void bindPrefListener(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        scheduleRefresh();
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null || key.startsWith("prefs_key_system_ui_volume_")) scheduleRefresh();
    }

    public void updatePreview() {
        if (mPreviewView == null) {
            LayoutPreference previewPref = findPreference("prefs_key_volume_style_preview");
            if (previewPref != null) {
                mPreviewView = previewPref.findViewById(R.id.volume_preview_view);
            }
        }
        if (mPreviewView == null) return;

        String preset = PrefsBridge.getString("system_ui_volume_style_preset", "oneui");
        boolean useCustomGeo = PrefsBridge.getBoolean("system_ui_volume_use_custom_geometry", false);
        boolean useCustomColor = PrefsBridge.getBoolean("system_ui_volume_oneui_custom_color", false);
        boolean useCustomBg = PrefsBridge.getBoolean("system_ui_volume_custom_bg", false);

        int defaultRadius;
        int defaultWidth;
        int defaultColor;
        int defaultBg;

        switch (preset.toLowerCase(Locale.ROOT)) {
            case "ios":
                defaultRadius = 18;
                defaultWidth = 46;
                defaultColor = 0xFFFFFFFF;
                defaultBg = 0x4D000000;
                break;
            case "pixel":
                defaultRadius = 32;
                defaultWidth = 60;
                defaultColor = 0xFF6750A4;
                defaultBg = 0x33000000;
                break;
            case "hyperos":
                defaultRadius = 22;
                defaultWidth = 46;
                defaultColor = 0xFF1875F0;
                defaultBg = 0x26000000;
                break;
            case "custom":
                defaultRadius = 28;
                defaultWidth = 52;
                defaultColor = 0xFF3478F6;
                defaultBg = 0x33000000;
                break;
            case "oneui":
            default:
                defaultRadius = 28;
                defaultWidth = 52;
                defaultColor = 0xFF3478F6;
                defaultBg = 0x33000000;
                break;
        }

        int radius = (useCustomGeo || "custom".equalsIgnoreCase(preset))
            ? PrefsBridge.getInt("system_ui_volume_oneui_radius", defaultRadius) : defaultRadius;
        int width = (useCustomGeo || "custom".equalsIgnoreCase(preset))
            ? PrefsBridge.getInt("system_ui_volume_slider_width", defaultWidth) : defaultWidth;
        int color = (useCustomColor || "custom".equalsIgnoreCase(preset))
            ? PrefsBridge.getInt("system_ui_volume_oneui_color", defaultColor) : defaultColor;
        int bg = (useCustomBg || "custom".equalsIgnoreCase(preset))
            ? PrefsBridge.getInt("system_ui_volume_bg_color", defaultBg) : defaultBg;

        mPreviewView.updateConfig(preset, radius, width, color, bg);
        mPreviewView.setAlpha(PrefsBridge.getBoolean("system_ui_volume_oneui_style", false) ? 1f : 0.5f);
    }
}
