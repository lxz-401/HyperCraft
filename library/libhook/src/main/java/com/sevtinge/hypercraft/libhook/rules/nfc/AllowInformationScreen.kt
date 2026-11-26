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
package com.sevtinge.hypercraft.libhook.rules.nfc

import com.sevtinge.hypercraft.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

class AllowInformationScreen : BaseHook() {
    override fun init() {
        findClass("com.android.nfc.NfcService").findMethod { name("sendMessage") }.createBeforeHook {
                if (it.args[1] == 2 || it.args[1] == 4) {
                    it.args[1] = 8
                }
            }
    }
}
