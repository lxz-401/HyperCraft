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
import com.sevtinge.hypercraft.libhook.rules.scanner.EnableCard;
import com.sevtinge.hypercraft.libhook.rules.scanner.EnableDocPpt;
import com.sevtinge.hypercraft.libhook.rules.scanner.EnableOcr;
import com.sevtinge.hypercraft.libhook.rules.scanner.EnableTranslation;
import com.sevtinge.hypercraft.libhook.rules.scanner.document.EnableDocument;
import com.sevtinge.hypercraft.libhook.rules.scanner.document.EnableExcel;
import com.sevtinge.hypercraft.libhook.rules.scanner.document.EnablePpt;

@HookBase(targetPackage = "com.xiaomi.scanner")
public class Scanner extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new EnableOcr(), PrefsBridge.getBoolean("scanner_ocr"));
        initHook(new EnableExcel(), PrefsBridge.getBoolean("scanner_excel"));
        initHook(new EnablePpt(), PrefsBridge.getBoolean("scanner_ppt"));
        initHook(new EnableCard(), PrefsBridge.getBoolean("scanner_card"));
        initHook(new EnableTranslation(), PrefsBridge.getBoolean("scanner_translation"));
        initHook(new EnableDocument(), PrefsBridge.getBoolean("scanner_document"));
        initHook(new EnableDocPpt(), PrefsBridge.getBoolean("scanner_doc_ppt"));
    }
}
