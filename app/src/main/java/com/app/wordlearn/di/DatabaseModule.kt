package com.app.wordlearn.di

import android.content.Context
import androidx.room.Room
import com.app.wordlearn.data.local.AppDatabase
import com.app.wordlearn.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wordlearn_db"
        ).build()
    }

    @Provides
    fun provideWordDao(database: AppDatabase): WordDao = database.wordDao()

    @Provides
    fun provideWordSampleDao(database: AppDatabase): WordSampleDao = database.wordSampleDao()

    @Provides
    fun provideWordProgressDao(database: AppDatabase): WordProgressDao = database.wordProgressDao()

    @Provides
    fun provideQuizSessionDao(database: AppDatabase): QuizSessionDao = database.quizSessionDao()

    @Provides
    fun provideQuizAnswerDao(database: AppDatabase): QuizAnswerDao = database.quizAnswerDao()

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()
}
