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
package com.sevtinge.hypercraft.libhook.rules.systemframework.volume;

import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class VolumeDefaultStream extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {
        mAudioService = findClassIfExists("com.android.server.audio.AudioService");

        findAndHookMethod(mAudioService, "getActiveStreamType", int.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {

                int stream = PrefsBridge.getStringAsInt("system_framework_default_volume_stream", 0);
                if (stream != 0) param.setResult(stream);

            }
        });
    }
}
