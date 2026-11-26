/*
 * This file is part of HyperCraft.
 *
 * HyperCraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.sevtinge.hypercraft.hooker.home;

import com.sevtinge.hypercraft.core.R;
import com.sevtinge.hypercraft.dashboard.DashboardFragment;

/** HyperOS 4 launcher animation controls, applied through the launcher's native ratio hook. */
public class HomeAnimationSettings extends DashboardFragment {
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_animation;
    }
}
