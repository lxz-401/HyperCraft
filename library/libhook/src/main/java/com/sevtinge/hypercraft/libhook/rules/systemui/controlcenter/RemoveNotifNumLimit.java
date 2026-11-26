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
package com.sevtinge.hypercraft.libhook.rules.systemui.controlcenter;

import com.sevtinge.hypercraft.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class RemoveNotifNumLimit extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator", "attach", "com.android.systemui.statusbar.notification.collection.NotifPipeline", new IReplaceHook() {
            @Override
            public Object replace(HookParam param) {
                return null;
            }
        });

        try {
            findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator$$ExternalSyntheticLambda0", "onViewBound", "com.android.systemui.statusbar.notification.collection.NotificationEntry", new IReplaceHook() {
                @Override
                public Object replace(HookParam param) {
                    return null;
                }
            });
        } catch (Throwable t) {
            findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator$$ExternalSyntheticLambda0", "onViewBound$1", "com.android.systemui.statusbar.notification.collection.NotificationEntry", new IReplaceHook() {
                @Override
                public Object replace(HookParam param) {
                    return null;
                }
            });
        }

    }
}
