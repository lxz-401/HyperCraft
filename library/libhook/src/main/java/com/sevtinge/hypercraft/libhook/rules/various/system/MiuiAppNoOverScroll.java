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
package com.sevtinge.hypercraft.libhook.rules.various.system;

import android.view.View;

import com.sevtinge.hypercraft.common.log.XposedLog;
import com.sevtinge.hypercraft.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class MiuiAppNoOverScroll extends BaseHook {


    @Override
    public void init() {

        Class<?> mSpringBackCls = findClassIfExists("miuix.springback.view.SpringBackLayout");
        Class<?> mRemixRvCls = findClassIfExists("androidx.recyclerview.widget.RemixRecyclerView");

        try {
            IMethodHook hookParam = new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    com.sevtinge.hypercraft.libhook.base.BaseHook.setBooleanField(param.getThisObject(), "mSpringBackEnable", false);
                    param.getArgs()[0] = false;
                }
            };

            if (mSpringBackCls != null) {

                hookAllConstructors(mSpringBackCls, new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        com.sevtinge.hypercraft.libhook.base.BaseHook.setBooleanField(param.getThisObject(), "mSpringBackEnable", false);
                    }
                });

                findAndHookMethod(mSpringBackCls, "setSpringBackEnable", boolean.class, hookParam);
            }


            if (mRemixRvCls != null) {
                hookAllConstructors(mRemixRvCls, new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        ((View) param.getThisObject()).setOverScrollMode(View.OVER_SCROLL_NEVER);
                        com.sevtinge.hypercraft.libhook.base.BaseHook.setBooleanField(param.getThisObject(), "mSpringBackEnable", false);
                    }
                });
                findAndHookMethod(mRemixRvCls, "setSpringEnabled", boolean.class, hookParam);
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "TAG" + getPackageName(), e);
        }
    }
}
