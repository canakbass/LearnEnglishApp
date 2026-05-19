package com.app.wordlearn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.app.wordlearn.data.local.dao.QuizAnswerDao
import com.app.wordlearn.data.local.dao.QuizSessionDao
import com.app.wordlearn.data.local.dao.SettingsDao
import com.app.wordlearn.data.local.dao.WordDao
import com.app.wordlearn.data.local.dao.WordProgressDao
import com.app.wordlearn.data.local.dao.WordSampleDao
import com.app.wordlearn.data.local.dao.StoryDao
import com.app.wordlearn.data.local.entity.QuizAnswerEntity
import com.app.wordlearn.data.local.entity.QuizSessionEntity
import com.app.wordlearn.data.local.entity.SettingsEntity
import com.app.wordlearn.data.local.entity.WordEntity
import com.app.wordlearn.data.local.entity.WordProgressEntity
import com.app.wordlearn.data.local.entity.WordSampleEntity
import com.app.wordlearn.data.local.entity.StoryEntity

@Database(
    entities = [
        WordEntity::class,
        WordSampleEntity::class,
        WordProgressEntity::class,
        QuizSessionEntity::class,
        QuizAnswerEntity::class,
        SettingsEntity::class,
        StoryEntity::class
    ],
    // v4: UserEntity/UserDao kaldırıldı (kullanılmıyordu, kimlik Firebase'de tutuluyor)
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun wordSampleDao(): WordSampleDao
    abstract fun wordProgressDao(): WordProgressDao
    abstract fun quizSessionDao(): QuizSessionDao
    abstract fun quizAnswerDao(): QuizAnswerDao
    abstract fun settingsDao(): SettingsDao
    abstract fun storyDao(): StoryDao

    /**
     * Kullanıcıya özgü tüm tabloları tek bir transaction içinde temizler.
     * DAO katmanı üzerinden çalışır → Room invalidation tracker tetiklenir,
     * dinleyen Flow'lar güncel verisi yansıtır.
     */
    suspend fun clearUserData() = withTransaction {
        wordProgressDao().deleteAll()
        quizSessionDao().deleteAll()
        quizAnswerDao().deleteAll()
        settingsDao().deleteAll()
        storyDao().clearAllStories()
    }
}
