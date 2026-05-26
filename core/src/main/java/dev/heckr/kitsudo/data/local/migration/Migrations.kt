package dev.heckr.kitsudo.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: adds parentId, deadlineAt, sortOrder to the tasks table.
 *
 * Existing rows get parentId = NULL (top-level), deadlineAt = NULL (no deadline),
 * sortOrder = 0.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tasks ADD COLUMN parentId TEXT")
        database.execSQL("ALTER TABLE tasks ADD COLUMN deadlineAt INTEGER")
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/**
 * v2 → v3: adds the priority column.
 *
 * Existing rows default to 0 (Priority.NORMAL).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0",
        )
    }
}
