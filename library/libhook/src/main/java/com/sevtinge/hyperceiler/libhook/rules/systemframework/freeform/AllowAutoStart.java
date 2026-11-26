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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform;

import android.content.Intent;
import android.content.pm.ApplicationInfo;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AllowAutoStart extends BaseHook {
    private final ThreadLocal<Deque<String>> callees = ThreadLocal.withInitial(ArrayDeque::new);

    @Override
    public void init() {
        findAndHookMethod("miui.app.ActivitySecurityHelper", "getCheckStartActivityIntent", ApplicationInfo.class, ApplicationInfo.class, Intent.class, boolean.class, int.class, boolean.class, int.class, int.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                ApplicationInfo callee = (ApplicationInfo) param.getArgs()[1];
                callees.get().push(callee != null && callee.packageName != null ? callee.packageName : "");
            }

            @Override
            public void after(HookParam param) {
                Deque<String> stack = callees.get();
                if (!stack.isEmpty()) stack.pop();
                if (stack.isEmpty()) callees.remove();
            }
        });

        findAndHookMethod("miui.app.ActivitySecurityHelper", "restrictForChain", ApplicationInfo.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Deque<String> stack = callees.get();
                String calleePackage = stack.peek();
                if (calleePackage == null) {
                    callees.remove();
                    return;
                }
                if (calleePackage.isEmpty()) return;
                Set<String> selectedApps = PrefsBridge.getStringSet("system_framework_auto_start_apps");
                if (selectedApps.contains(calleePackage)) {
                    ApplicationInfo caller = (ApplicationInfo) param.getArgs()[0];
                    XposedLog.d(TAG, "Boot has been allowed! caller: "
                        + (caller != null ? caller.packageName : "unknown") + " callee: " + calleePackage);
                    param.setResult(false);
                }
            }
        });
    }
}
