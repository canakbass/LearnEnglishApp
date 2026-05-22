package com.app.wordlearn.di

import com.app.wordlearn.data.repository.ProgressRepositoryImpl
import com.app.wordlearn.data.repository.SessionRepositoryImpl
import com.app.wordlearn.data.repository.SettingsRepositoryImpl
import com.app.wordlearn.data.repository.StoryRepositoryImpl
import com.app.wordlearn.data.repository.UserRepositoryImpl
import com.app.wordlearn.data.repository.WordRepositoryImpl
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SessionRepository
import com.app.wordlearn.domain.repository.SettingsRepository
import com.app.wordlearn.domain.repository.StoryRepository
import com.app.wordlearn.domain.repository.UserRepository
import com.app.wordlearn.domain.repository.WordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt @Binds modülü — `interface` kullanılır (abstract class yerine).
 * Kotlin'de @Binds için abstract class gereksiz; interface daha sade ve
 * SonarQube `kotlin:S6517` ("convert to interface") uyarısını giderir.
 */
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindWordRepository(impl: WordRepositoryImpl): WordRepository

    @Binds
    @Singleton
    fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository
}
