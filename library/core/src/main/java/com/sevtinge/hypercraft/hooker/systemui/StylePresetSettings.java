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
package com.sevtinge.hypercraft.hooker.systemui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.core.R;
import com.sevtinge.hypercraft.dashboard.DashboardFragment;
import com.sevtinge.hypercraft.libhook.rules.systemui.style.StylePreset;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Style preset manager screen: apply a built-in style bundle in one step,
 * and export/import the current style settings as a JSON file.
 */
public class StylePresetSettings extends DashboardFragment
    implements Preference.OnPreferenceChangeListener {

    private static final int REQ_IMPORT = 1001;
    private static final int REQ_EXPORT = 1002;
    private static final String MIME_JSON = "application/json";
    private static final String DEFAULT_FILE_NAME = "hypercraft_style_preset.json";

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_style_presets;
    }

    @Override
    public void initPrefs() {
        Preference preset = findPreference("prefs_key_style_preset");
        if (preset != null) {
            preset.setOnPreferenceChangeListener(this);
        }
        Preference export = findPreference("prefs_key_style_preset_export");
        if (export != null) {
            export.setOnPreferenceClickListener(p -> {
                startExport();
                return true;
            });
        }
        Preference importPref = findPreference("prefs_key_style_preset_import");
        if (importPref != null) {
            importPref.setOnPreferenceClickListener(p -> {
                startImport();
                return true;
            });
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        if ("prefs_key_style_preset".equals(preference.getKey())) {
            StylePreset.byId(String.valueOf(newValue)).apply();
        }
        return true;
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_JSON);
        intent.putExtra(Intent.EXTRA_TITLE, DEFAULT_FILE_NAME);
        startActivityForResult(intent, REQ_EXPORT);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (requestCode == REQ_EXPORT) {
            writePreset(data.getData());
        } else if (requestCode == REQ_IMPORT) {
            readPreset(data.getData());
        }
    }

    private void writePreset(Uri uri) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("source", "HyperCraft");
            root.put("values", buildExportValues());
            byte[] json = root.toString().getBytes(StandardCharsets.UTF_8);
            try (var out = requireContext().getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    return;
                }
                out.write(json);
            }
            Toast.makeText(requireContext(), R.string.style_preset_export, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.style_preset_io_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads the current values of every export key through typed
     * [PrefsBridge] getters. The export file is therefore deterministic:
     * each key is written back with the same type on import.
     */
    private JSONObject buildExportValues() {
        JSONObject values = new JSONObject();
        try {
            values.put("system_ui_statusbar_clock_all_status_enable",
                PrefsBridge.getBoolean("system_ui_statusbar_clock_all_status_enable"));
            values.put("system_ui_statusbar_clock_style",
                PrefsBridge.getStringAsInt("system_ui_statusbar_clock_style", 0));
            values.put("system_ui_statusbar_clock_color_mode",
                PrefsBridge.getStringAsInt("system_ui_statusbar_clock_color_mode", 0));
            values.put("system_ui_statusbar_clock_editor_s",
                PrefsBridge.getString("system_ui_statusbar_clock_editor_s", "HH:mm"));
            values.put("system_ui_statusbar_clock_editor_b",
                PrefsBridge.getString("system_ui_statusbar_clock_editor_b", "HH:mm"));
            values.put("system_ui_statusbar_battery_percent_enable",
                PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_enable"));
            values.put("system_ui_statusbar_battery_percent_at_right",
                PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_at_right"));
            values.put("system_ui_statusbar_battery_percent_show_sign",
                PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_show_sign", true));
            values.put("system_ui_statusbar_battery_detail_enable",
                PrefsBridge.getBoolean("system_ui_statusbar_battery_detail_enable"));
            values.put("system_ui_volume_oneui_style",
                PrefsBridge.getBoolean("system_ui_volume_oneui_style"));
            values.put("system_ui_volume_style_preset",
                PrefsBridge.getString("system_ui_volume_style_preset", "oneui"));
            values.put("system_control_center_old_enable",
                PrefsBridge.getBoolean("system_control_center_old_enable"));
        } catch (Exception ignored) {
            // PrefsBridge not initialized yet; export an empty preset set
        }
        return values;
    }

    private void readPreset(Uri uri) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(requireContext().getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONObject values = root.optJSONObject("values");
            if (values == null) {
                throw new Exception("no values");
            }
            applyImportedValues(values);
            Toast.makeText(requireContext(), R.string.style_preset_import, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.style_preset_invalid, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Applies the imported values; only keys from the stable export set are
     * accepted, so a file can never write arbitrary preference keys.
     */
    private void applyImportedValues(JSONObject values) {
        Set<String> allowed = new LinkedHashSet<>(StylePreset.EXPORT_KEYS);
        JSONArray names = values.names();
        if (names == null) {
            return;
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            if (key.isEmpty() || !allowed.contains(key)) {
                continue;
            }
            Object value = values.opt(key);
            if (value instanceof Boolean b) {
                PrefsBridge.putBoolean(key, b);
            } else if (value instanceof Number n) {
                double d = n.doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) <= Integer.MAX_VALUE) {
                    PrefsBridge.putInt(key, (int) d);
                } else {
                    PrefsBridge.putFloat(key, (float) d);
                }
            } else if (value instanceof String s) {
                PrefsBridge.putString(key, s);
            } else if (value instanceof JSONArray arr) {
                Set<String> set = new LinkedHashSet<>();
                for (int j = 0; j < arr.length(); j++) {
                    set.add(String.valueOf(arr.opt(j)));
                }
                PrefsBridge.putStringSet(key, set);
            }
        }
    }
}
