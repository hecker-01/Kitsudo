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

/**
 * v3 → v4: adds recurrence columns.
 *
 * Existing rows get recurrenceUnit = NULL (non-recurring) and
 * recurrenceInterval = 1 (the default period count).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceUnit TEXT")
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1",
        )
    }
}

/**
 * v4 → v5: adds the tags table and the task-to-tag join table (many-to-many).
 *
 * Both join foreign keys cascade on delete so removing a task or a tag clears
 * its assignments. Column types / index names match Room's generated schema.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`color` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `task_tag_cross_ref` (" +
                "`taskId` TEXT NOT NULL, " +
                "`tagId` TEXT NOT NULL, " +
                "PRIMARY KEY(`taskId`, `tagId`), " +
                "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_task_tag_cross_ref_taskId` " +
                "ON `task_tag_cross_ref` (`taskId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_task_tag_cross_ref_tagId` " +
                "ON `task_tag_cross_ref` (`tagId`)",
        )
    }
}
