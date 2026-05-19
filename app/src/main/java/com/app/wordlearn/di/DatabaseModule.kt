package com.app.wordlearn.di

import android.content.Context
import androidx.room.Room
import com.app.wordlearn.data.local.AppDatabase
import com.app.wordlearn.data.local.MIGRATION_3_4
import com.app.wordlearn.data.local.dao.QuizAnswerDao
import com.app.wordlearn.data.local.dao.QuizSessionDao
import com.app.wordlearn.data.local.dao.SettingsDao
import com.app.wordlearn.data.local.dao.WordDao
import com.app.wordlearn.data.local.dao.WordProgressDao
import com.app.wordlearn.data.local.dao.WordSampleDao
import com.app.wordlearn.domain.util.Constants
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
            Constants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_3_4)
            .build()
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

    @Provides
    fun provideStoryDao(database: AppDatabase): com.app.wordlearn.data.local.dao.StoryDao = database.storyDao()
}
