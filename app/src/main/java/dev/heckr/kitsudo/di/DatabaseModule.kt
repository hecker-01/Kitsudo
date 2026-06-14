package dev.heckr.kitsudo.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.heckr.kitsudo.data.local.KitsudoDatabase
import dev.heckr.kitsudo.data.local.dao.TagDao
import dev.heckr.kitsudo.data.local.dao.TaskDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KitsudoDatabase =
        Room.databaseBuilder(context, KitsudoDatabase::class.java, "kitsudo.db")
            .addMigrations(*KitsudoDatabase.migrations)
            .build()

    @Provides
    @Singleton
    fun provideTaskDao(db: KitsudoDatabase): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideTagDao(db: KitsudoDatabase): TagDao = db.tagDao()
}
