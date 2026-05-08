package dev.heckr.kitsudo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.heckr.kitsudo.data.preferences.ThemeRepositoryImpl
import dev.heckr.kitsudo.data.repository.TaskRepositoryImpl
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
