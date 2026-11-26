/*
 * This file is part of HyperCraft.

 * HyperCraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCraft Contributions
 */
package com.sevtinge.hypercraft.libhook.app.Others;

import static com.sevtinge.hypercraft.libhook.base.BaseHook.findClass;
import static com.sevtinge.hypercraft.libhook.base.BaseHook.setStaticObjectField;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.hchen.database.HookBase;
import com.sevtinge.hypercraft.common.log.XposedLog;
import com.sevtinge.hypercraft.common.log.ModuleLog;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.appbase.input.InputMethodClassLoaderDispatcher;
import com.sevtinge.hypercraft.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.various.MusicHooks;
import com.sevtinge.hypercraft.libhook.rules.various.clipboard.BaiduClipboard;
import com.sevtinge.hypercraft.libhook.rules.various.clipboard.SoGouClipboard;
import com.sevtinge.hypercraft.libhook.rules.various.clipboard.UnlockIme;
import com.sevtinge.hypercraft.libhook.rules.various.input.MiAospIme;
import com.sevtinge.hypercraft.libhook.utils.hookapi.tool.AppsTool;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@HookBase(targetPackage = "VariousThirdApps")
public class VariousThirdApps extends BaseLoad {
    private static final String XIAOMI_SOGOU_PACKAGE = "com.sohu.inputmethod.sogou.xiaomi";
    private static final String SOGOU_PACKAGE = "com.sohu.inputmethod.sogou";
    private static final String XIAOMI_BAIDU_PACKAGE = "com.baidu.input_mi";
    private static final String BAIDU_PACKAGE = "com.baidu.input";

    private static Set<String> sEnabledInputMethodPackages = Collections.emptySet();

    private String mPackageName;


    @Override
    public void onPackageLoaded() {
        mPackageName = getPackageName();
        // Selected IMEs must not depend on a one-shot system-list query during
        // cold start: the context/service may not be ready, or the list filtered.
        boolean selectedInputMethod = InputMethodConfig.shouldHookMiuiIme(mPackageName)
            || InputMethodConfig.shouldHookAospIme(mPackageName);
        boolean isInputMethod = selectedInputMethod || isEnabledInputMethodPackage(mPackageName);

        if (isInputMethod) {
            if (selectedInputMethod) {
                ModuleLog.i("HyperCraft.ImeBottom", "Installing selected IME hooks package=" + mPackageName
                    + " style=" + InputMethodConfig.getImeStyle());
            }
            initInputMethodHooks();
            initClipboardHooks();
            return;
        }

        if ("com.google.android.googlequicksearchbox".equals(mPackageName)) {
            try {
                Class<?> mBuild = findClass("android.os.Build", getClassLoader());

                setStaticObjectField(mBuild, "MANUFACTURER", "Google");
                setStaticObjectField(mBuild, "BRAND", "google");
                setStaticObjectField(mBuild, "MODEL", "Pixel 9 Pro");
                setStaticObjectField(mBuild, "DEVICE", "caiman");
                XposedLog.d("GoogleQuickSearchBox", "Spoofed device info to Pixel 9 Pro success");
            } catch (Throwable e) {
                XposedLog.e("GoogleQuickSearchBox", "Failed to spoof device info: " + e.getMessage());
            }
            return;
        }

        initMusicHooks();
    }

    private void initInputMethodHooks() {
        boolean needMiuiImeUnlock = InputMethodConfig.shouldHookMiuiIme(mPackageName);
        boolean needAospIme = InputMethodConfig.shouldHookAospIme(mPackageName);

        initHook(new InputMethodClassLoaderDispatcher());
        initHook(new UnlockIme(), needMiuiImeUnlock);
        initHook(new MiAospIme(), needAospIme);
    }

    private void initClipboardHooks() {
        boolean enableClipboardHook = PrefsBridge.getBoolean("sogou_xiaomi_clipboard");

        initHook(new SoGouClipboard(), enableClipboardHook && isSogouPackage(mPackageName));
        initHook(new BaiduClipboard(), enableClipboardHook && isBaiduPackage(mPackageName));
    }

    private void initMusicHooks() {
        initHook(MusicHooks.INSTANCE, PrefsBridge.getBoolean("system_ui_statusbar_music_switch") && PrefsBridge.getBoolean("system_ui_statusbar_music_show_app"));
    }
    private Set<String> getEnabledInputMethodPackages(Context context) {
        try {
            if (context == null) {
                XposedLog.e("getEnabledInputMethodPackages", "context is null");
                return Collections.emptySet();
            }

            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager == null) {
                XposedLog.e("getEnabledInputMethodPackages", "inputMethodManager is null");
                return Collections.emptySet();
            }

            List<InputMethodInfo> enabledInputMethods = inputMethodManager.getEnabledInputMethodList();
            LinkedHashSet<String> packages = new LinkedHashSet<>();
            for (InputMethodInfo inputMethodInfo : enabledInputMethods) {
                if (inputMethodInfo.getServiceInfo() != null) {
                    packages.add(inputMethodInfo.getServiceInfo().packageName);
                }
            }
            return packages;
        } catch (Throwable e) {
            XposedLog.e("getEnabledInputMethodPackages", "have e: " + e + ", message: " + e.getMessage());
            return Collections.emptySet();
        }
    }

    private boolean isEnabledInputMethodPackage(String packageName) {
        if (sEnabledInputMethodPackages.isEmpty()) {
            sEnabledInputMethodPackages = getEnabledInputMethodPackages(
                AppsTool.findContext(AppsTool.FlAG_ONLY_ANDROID));
        }
        return sEnabledInputMethodPackages.contains(packageName);
    }

    private boolean isSogouPackage(String packageName) {
        return XIAOMI_SOGOU_PACKAGE.equals(packageName) || SOGOU_PACKAGE.equals(packageName);
    }

    private boolean isBaiduPackage(String packageName) {
        return BAIDU_PACKAGE.equals(packageName) || XIAOMI_BAIDU_PACKAGE.equals(packageName);
    }
}
