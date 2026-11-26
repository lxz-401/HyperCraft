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

import android.os.Bundle;

import com.sevtinge.hypercraft.prefs.RecommendPreference;
import com.sevtinge.hypercraft.dashboard.DashboardFragment;
import com.sevtinge.hypercraft.libhook.R;

public class NfcFragment extends DashboardFragment {

    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hypercraft.core.R.xml.nfc;
    }

    @Override
    public void initPrefs() {

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_tsmclient_auto_nfc");
        args1.putInt(":settings:fragment_resId", com.sevtinge.hypercraft.core.R.xml.tsmclient);
        mRecommend.addRecommendView(getString(R.string.tsmclient_auto_nfc),
                null,
                DashboardFragment.class,
                args1,
                R.string.tsmclient
        );

    }
}
