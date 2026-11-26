/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class BackupUtils {
    public static final int CREATE_DOCUMENT_CODE = 255774;
    public static final int OPEN_DOCUMENT_CODE = 277451;
    public static final String BACKUP_FILE_NAME = "HyperCeiler_settings_backup";
    // JSON numbers do not retain SharedPreferences' Float/Long types (1.0f and 1L parse as Integer).
    private static final String NUMBER_TYPE_KEY = "__hyperceiler_numeric_type";
    private static final String NUMBER_VALUE_KEY = "value";
    private static final String KEY_ALLOW_HOOK = "prefs_key_allow_hook";
    private static final String KEY_FRAMEWORK_ALLOW_HOOK = "prefs_key_framework_api_allow_hook";
    private static final Set<String> RUNTIME_FRAMEWORK_KEYS = Set.of(
        "prefs_key_framework_check_reason",
        "prefs_key_framework_check_name",
        "prefs_key_framework_check_version",
        "prefs_key_framework_check_version_code",
        "prefs_key_framework_check_api_version",
        "prefs_key_framework_check_detail"
    );

    // 获取备份用的 Intent
    public static Intent getCreateDocumentIntent() {
        @SuppressLint("SimpleDateFormat")
        String backupFileName = BACKUP_FILE_NAME + new SimpleDateFormat("_yyyy-MM-dd-HH:mm:ss").format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, backupFileName);
        return intent;
    }

    // 获取恢复用的 Intent
    public static Intent getOpenDocumentIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        return intent;
    }


    /**
     * 执行备份：强制从物理句柄读取，确保文件不为空
     */
    public static void handleCreateDocument(Context context, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;

        // 关键：强制获取物理文件中的所有配置
        Map<String, ?> allEntries = PrefsBridge.getAll();
        AndroidLog.d("Backup", "开始备份，读取到项数: " + allEntries.size());

        try (OutputStream os = context.getContentResolver().openOutputStream(data)) {
            if (os == null) throw new IOException("Unable to open backup output stream");
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {

                JSONObject jsonObject = new JSONObject();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    String key = entry.getKey();
                    if (shouldSkipKey(key)) continue;

                    Object value = entry.getValue();
                    // 针对 StringSet 做特殊 JSON 处理，存为 JSONArray
                    if (value instanceof Set) {
                        jsonObject.put(key, new JSONArray((Set<?>) value));
                    } else if (value instanceof Float || value instanceof Long) {
                        jsonObject.put(key, new JSONObject()
                            .put(NUMBER_TYPE_KEY, value instanceof Float ? "float" : "long")
                            .put(NUMBER_VALUE_KEY, value.toString()));
                    } else {
                        jsonObject.put(key, value);
                    }
                }

                writer.write(jsonObject.toString(4)); // 使用缩进增加可读性
                writer.flush();
            }
        }
    }

    /**
     * 执行恢复：双写到物理和远程
     */
    public static void handleReadDocument(Context context, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;

        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(data)) {
            if (is == null) throw new IOException("Unable to open backup input stream");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
        }

        JSONObject jsonObject = new JSONObject(sb.toString());
        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            if (shouldSkipKey(key)) continue;

            Object value = jsonObject.get(key);

            switch (value) {
                case JSONArray jsonArray -> PrefsBridge.putByApp(key, jsonArrayToStringSet(jsonArray));
                case JSONObject typedNumber -> restoreTypedNumber(key, typedNumber);
                case String str -> PrefsBridge.putByApp(key, str);
                case Boolean b -> PrefsBridge.putByApp(key, b);
                case Integer i -> PrefsBridge.putByApp(key, i);
                case Long l -> PrefsBridge.putByApp(key, l);
                case Number number -> PrefsBridge.putByApp(key, number.floatValue());
                default -> {
                }
            }
        }

    }

    public static void handleCreateDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        handleCreateDocument((Context) activity, data);
    }

    public static void handleReadDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        handleReadDocument((Context) activity, data);
    }

    private static void restoreTypedNumber(String key, JSONObject encoded) throws JSONException {
        String type = encoded.optString(NUMBER_TYPE_KEY, "");
        if (!"float".equals(type) && !"long".equals(type)) return;
        if (!(encoded.get(NUMBER_VALUE_KEY) instanceof String value)) {
            throw new JSONException("Invalid typed numeric preference: " + key);
        }
        try {
            if ("float".equals(type)) {
                PrefsBridge.putByApp(key, Float.parseFloat(value));
            } else {
                PrefsBridge.putByApp(key, Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            throw new JSONException("Invalid typed numeric preference: " + key);
        }
    }

    private static boolean shouldSkipKey(String key) {
        return KEY_ALLOW_HOOK.equals(key)
            || KEY_FRAMEWORK_ALLOW_HOOK.equals(key)
            || RUNTIME_FRAMEWORK_KEYS.contains(key);
    }

    private static Set<String> jsonArrayToStringSet(JSONArray jsonArray) throws JSONException {
        Set<String> stringSet = new LinkedHashSet<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String item = jsonArray.optString(i, null);
            if (item != null) {
                stringSet.add(item);
            }
        }
        return stringSet;
    }
}
