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
package com.sevtinge.hypercraft.hooker;

import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.Miui.isPad;

import androidx.preference.Preference;

import com.sevtinge.hypercraft.core.R;
import com.sevtinge.hypercraft.dashboard.DashboardFragment;

public class SystemFrameworkFragment extends DashboardFragment {

    Preference mMipad; // 平板相关功能

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.framework;
    }

    @Override
    public void initPrefs() {
        mMipad = findPreference("prefs_key_system_framework_mipad");
        mMipad.setVisible(isPad());
    }
}
