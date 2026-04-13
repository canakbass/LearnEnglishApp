package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.Settings

interface SettingsRepository {
    suspend fun getSettings(): Settings
    suspend fun getDailyNewWordCount(): Int
    suspend fun setDailyNewWordCount(count: Int)
    suspend fun getUserLevel(): String
    suspend fun setUserLevel(level: String)
    suspend fun saveSettings(settings: Settings)
}
