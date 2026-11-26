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
package com.sevtinge.hypercraft.log.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LogEntry.class, com.sevtinge.hypercraft.diagnostics.DiagnosticSnapshotEntity.class}, version = 3, exportSchema = false)
public abstract class LogDatabase extends RoomDatabase {
    public abstract LogDao logDao();
    public abstract com.sevtinge.hypercraft.diagnostics.DiagnosticDao diagnosticDao();
    public static final androidx.room.migration.Migration DIAGNOSTICS_MIGRATION =
        new androidx.room.migration.Migration(2,3) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS hook_diagnostics (fingerprint TEXT NOT NULL PRIMARY KEY, timestamp INTEGER NOT NULL, report TEXT NOT NULL)");
            }
        };
}
