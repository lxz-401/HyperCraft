/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.hooker.systemui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hooker.systemui.widget.VolumeStylePreviewView;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;

public class VolumeStyleSettings extends DashboardFragment
    implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private VolumeStylePreviewView mPreviewView;

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

        updatePreview();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(this::updatePreview);
        try {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            if (sp != null) {
                sp.registerOnSharedPreferenceChangeListener(this);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            if (sp != null) {
                sp.unregisterOnSharedPreferenceChangeListener(this);
            }
        } catch (Exception ignored) {}
    }

    private void bindPrefListener(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (getView() != null) {
            getView().post(this::updatePreview);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (getView() != null) {
            getView().post(this::updatePreview);
        }
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

        switch (preset.toLowerCase()) {
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
    }
}
