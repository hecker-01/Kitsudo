package dev.heckr.kitsudo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.local.entity.TaskEntity
import dev.heckr.kitsudo.data.local.migration.MIGRATION_1_2
import dev.heckr.kitsudo.data.local.migration.MIGRATION_2_3

@Database(
    entities = [TaskEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class KitsudoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val migrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
