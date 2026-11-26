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
package com.sevtinge.hypercraft;

import static android.os.Process.killProcess;

import android.content.Context;
import android.os.Process;
import android.os.UserManager;

import androidx.annotation.NonNull;

import com.sevtinge.hypercraft.common.log.AndroidLog;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.home.banner.HomePageBannerManager;
import com.sevtinge.hypercraft.home.safemode.AppCrashStore;
import com.sevtinge.hypercraft.home.task.AppInitializer;
import com.sevtinge.hypercraft.log.LogManager;
import com.sevtinge.hypercraft.provision.fragment.PermissionSettingsFragment;
import com.sevtinge.hypercraft.utils.DeviceInfoBuilder;
import com.sevtinge.hypercraft.utils.FrameworkStatusManager;
import com.sevtinge.hypercraft.utils.LSPosedScopeHelper;
import com.sevtinge.hypercraft.utils.ScopeManager;
import com.sevtinge.hypercraft.utils.XposedActivateHelper;

import fan.provision.OobeUtils;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Application extends fan.app.Application
    implements XposedServiceHelper.OnServiceListener {

    private static final String TAG = "Application";
    public static volatile boolean isModuleActivated = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.sevtinge.hypercraft.utils.LanguageHelper.wrapContext(base));
        AppInitializer.attach(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用启动阶段，预热非 UI 任务（如 Shell、语言包、权限检查）
        AppInitializer.initOnAppCreate(this);
        syncHookAvailabilityIfUnlocked();
        FrameworkStatusManager.init();

        // A preferences Provider query can cold-start the app without opening its UI.
        // Keep the log repository ready, but never import historical logs on that path.
        LogManager.init(this, DeviceInfoBuilder::build, null);
        setupCrashHandler();
    }


    @Override
    public void onServiceBind(@NonNull XposedService service) {
        synchronized (this) {
            setModuleActivated(true);
            PrefsBridge.setRemotePrefs(service.getRemotePreferences(PrefsBridge.REMOTE_PREFS_GROUP));
            syncHookAvailabilityIfUnlocked();
            FrameworkStatusManager.onServiceBound(service);
            AndroidLog.d(TAG, "XposedService connected: " + describeFrameworkStatus());
            ScopeManager.setService(service);
            LSPosedScopeHelper.reloadScope();
            refreshHomePageBanner();
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.e(TAG, "XposedService died.");
        synchronized (this) {
            setModuleActivated(false);
            PrefsBridge.setRemotePrefs(null);
            FrameworkStatusManager.onServiceDied();
            ScopeManager.clearService();
            refreshHomePageBanner();
        }
    }

    private static void setModuleActivated(boolean activated) {
        isModuleActivated = activated;
        PermissionSettingsFragment.isModuleActive = activated;
        XposedActivateHelper.onActivationChanged();
    }

    private static void refreshHomePageBanner() {
        HomePageBannerManager.invalidateCache();
        HomePageBannerManager.requestRefresh();
    }

    private void syncHookAvailabilityIfUnlocked() {
        UserManager users = getSystemService(UserManager.class);
        if (users == null || users.isUserUnlocked()) OobeUtils.syncHookAvailability(this);
    }

    @NonNull
    private static String describeFrameworkStatus() {
        FrameworkStatusManager.Status status = FrameworkStatusManager.getCurrentStatus();
        String sb = valueOrUnknown(status.getFrameworkName()) +
            " v" +
            valueOrUnknown(status.getFrameworkVersion()) +
            " (API " +
            (status.getFrameworkApiVersion() >= 0 ? status.getFrameworkApiVersion() : "Unknown") +
            ")";
        return sb;
    }

    @NonNull
    private static String valueOrUnknown(String value) {
        return value == null || value.isEmpty() ? "Unknown" : value;
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            AppCrashStore.persist(getApplicationContext(), ex);
            AndroidLog.e("Crash", "App crash captured", ex);

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            } else {
                killProcess(Process.myPid());
                System.exit(1);
            }
        });
    }
}
