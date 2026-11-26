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
package com.sevtinge.hypercraft.home.helper;

import androidx.preference.Preference;

import com.sevtinge.hypercraft.core.R;
import com.sevtinge.hypercraft.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hypercraft.home.utils.HeaderManager;

public class CantSeeAppsFragment extends SettingsPreferenceFragment {

    private Preference mHelpCantSeeApps;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hypercraft.R.xml.prefs_help_cant_see_apps;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);
        mHelpCantSeeApps = findPreference("prefs_key_textview_help_cant_see_apps");
        if (mHelpCantSeeApps != null) {
            HeaderManager.HiddenReport report = HeaderManager.buildHiddenReport(requireContext());
            mHelpCantSeeApps.setSummary(HeaderManager.buildHiddenSummary(requireContext(), report));
        }
    }
}
