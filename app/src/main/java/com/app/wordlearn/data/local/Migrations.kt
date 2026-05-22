package com.app.wordlearn.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 → v4 migration:
 * - `users` tablosu kaldırıldı (UserEntity/UserDao silindi; kimlik Firebase'de tutuluyor).
 *
 * Diğer tablolar (`words`, `word_progress`, `quiz_sessions`, `quiz_answers`,
 * `settings`, `stories`) korunuyor — böylece kullanıcının kelime ilerlemesi, geçmiş
 * quiz oturumları ve ayarları schema bump'ında silinmez.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS users")
    }
}

/**
 * v4 → v5: word_progress tablosuna lastShownDate sütunu eklendi.
 * Kelimenin bugünkü quiz oturumuna eklendiği zamanı tutar;
 * quiz devam mantığı için kullanılır, istatistikleri etkilemez.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE word_progress ADD COLUMN lastShownDate INTEGER NOT NULL DEFAULT 0")
    }
}
