package dev.heckr.kitsudo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class KitsudoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
