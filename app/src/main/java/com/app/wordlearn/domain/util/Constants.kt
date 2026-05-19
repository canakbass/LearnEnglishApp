package com.app.wordlearn.domain.util

object Constants {
    const val DATABASE_NAME = "english_words_db"
    /** JSON tabanlı kelime seed dosyası (assets/). v4'te SQL parser'ın yerini aldı. */
    const val DATABASE_SEED_JSON = "system_words_seed.json"
    
    const val DEFAULT_WORD_LEVEL = "Orta"
    const val DEFAULT_CATEGORY = "Genel"
    const val SYSTEM_SOURCE = "system"
    const val USER_SOURCE = "user"
    
    const val MIN_PASSWORD_LENGTH = 6
    const val STREAK_THRESHOLD = 6
    const val DAY_IN_MS = 86_400_000L
    
    const val MAX_WORDLE_ATTEMPTS = 6
    const val WORDLE_WORD_LENGTH = 5
}
