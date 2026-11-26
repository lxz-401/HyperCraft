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
package com.sevtinge.hypercraft.libhook.rules.systemframework.others;

import com.sevtinge.hypercraft.libhook.base.BaseHook;

public class DisablePinVerifyPer72h extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.server.locksettings.LockSettingsStrongAuth", "rescheduleStrongAuthTimeoutAlarm", long.class, int.class, doNothing());
    }
}
