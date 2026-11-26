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
package com.sevtinge.hypercraft.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.libhook.base.BaseLoad;
import com.sevtinge.hypercraft.libhook.rules.screenrecorder.ForceSupportPlaybackCapture;
import com.sevtinge.hypercraft.libhook.rules.screenrecorder.SaveToMovies;
import com.sevtinge.hypercraft.libhook.rules.screenrecorder.ScreenRecorderConfig;
import com.sevtinge.hypercraft.libhook.rules.screenrecorder.UnlockMoreVolumeFromNew;

@HookBase(targetPackage = "com.miui.screenrecorder")
public class ScreenRecorder extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new ForceSupportPlaybackCapture(), PrefsBridge.getBoolean("screenrecorder_force_support_playback_capture"));
        initHook(UnlockMoreVolumeFromNew.INSTANCE, PrefsBridge.getBoolean("screenrecorder_more_volume"));
        initHook(new ScreenRecorderConfig(), PrefsBridge.getBoolean("screenrecorder_config"));
        initHook(SaveToMovies.INSTANCE, PrefsBridge.getBoolean("screenrecorder_save_to_movies"));
    }
}
