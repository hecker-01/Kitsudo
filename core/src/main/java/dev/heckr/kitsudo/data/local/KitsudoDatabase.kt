package dev.heckr.kitsudo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.heckr.kitsudo.data.local.dao.TagDao
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.local.entity.TagEntity
import dev.heckr.kitsudo.data.local.entity.TaskEntity
import dev.heckr.kitsudo.data.local.entity.TaskTagCrossRef
import dev.heckr.kitsudo.data.local.migration.MIGRATION_1_2
import dev.heckr.kitsudo.data.local.migration.MIGRATION_2_3
import dev.heckr.kitsudo.data.local.migration.MIGRATION_3_4
import dev.heckr.kitsudo.data.local.migration.MIGRATION_4_5

@Database(
    entities = [TaskEntity::class, TagEntity::class, TaskTagCrossRef::class],
    version = 5,
    exportSchema = true,
)
abstract class KitsudoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao

    companion object {
        val migrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
