/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.hooker.systemui;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class VolumeStyleSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_volume_style;
    }

    @Override
    public void initPrefs() {
    }
}
