package dev.heckr.kitsudo.wear.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.heckr.kitsudo.data.local.KitsudoDatabase
import dev.heckr.kitsudo.data.local.dao.TaskDao
import javax.inject.Singleton

/**
 * Provides the watch-local Room database. Uses a separate file name
 * ("kitsudo-wear.db") so it doesn't conflict with the phone's "kitsudo.db".
 * Data is populated (and replaced) by [dev.heckr.kitsudo.wear.data.sync.WearDataListenerService].
 */
@Module
@InstallIn(SingletonComponent::class)
object WearDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KitsudoDatabase =
        Room.databaseBuilder(context, KitsudoDatabase::class.java, "kitsudo-wear.db")
            .addMigrations(*KitsudoDatabase.migrations)
            .build()

    @Provides
    @Singleton
    fun provideTaskDao(db: KitsudoDatabase): TaskDao = db.taskDao()
}
