package com.app.wordlearn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.wordlearn.data.local.dao.*
import com.app.wordlearn.data.local.entity.*

@Database(
    entities = [
        WordEntity::class,
        WordSampleEntity::class,
        WordProgressEntity::class,
        QuizSessionEntity::class,
        QuizAnswerEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun wordSampleDao(): WordSampleDao
    abstract fun wordProgressDao(): WordProgressDao
    abstract fun quizSessionDao(): QuizSessionDao
    abstract fun quizAnswerDao(): QuizAnswerDao
    abstract fun settingsDao(): SettingsDao
}
