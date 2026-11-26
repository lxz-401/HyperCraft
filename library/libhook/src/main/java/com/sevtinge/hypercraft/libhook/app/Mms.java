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
import com.sevtinge.hypercraft.libhook.rules.mms.AutoCopyVerificationCode;
import com.sevtinge.hypercraft.libhook.rules.mms.DisableAd;
import com.sevtinge.hypercraft.libhook.rules.mms.DisableRiskTip;
import com.sevtinge.hypercraft.libhook.rules.mms.ImOldDevice;

@HookBase(targetPackage = "com.android.mms")
public class Mms extends BaseLoad {


    @Override
    public void onPackageLoaded() {
        initHook(new DisableRiskTip(), PrefsBridge.getBoolean("mms_disable_fraud_risk_tip") || PrefsBridge.getBoolean("mms_disable_overseas_risk_tip"));
        initHook(new DisableAd(), PrefsBridge.getBoolean("mms_disable_ad"));
        initHook(new ImOldDevice(), PrefsBridge.getBoolean("mms_im_old_device"));
        initHook(AutoCopyVerificationCode.INSTANCE, PrefsBridge.getBoolean("mms_verification_code_auto_copy"));
    }
}
