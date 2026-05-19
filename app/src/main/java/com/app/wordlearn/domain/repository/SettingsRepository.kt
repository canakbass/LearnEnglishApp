package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.Settings

interface SettingsRepository {
    suspend fun getSettings(): Settings
    suspend fun getDailyNewWordCount(): Int
    suspend fun setDailyNewWordCount(count: Int)
    suspend fun getUserLevel(): String
    suspend fun setUserLevel(level: String)
    suspend fun saveSettings(settings: Settings)
    /**
     * Bugünün günlük kelime kotasını döner.
     * İlk çağrıda (ya da gün değiştiyse) o anki ayarı kilitlere;
     * aynı gün içindeki sonraki çağrılarda kilitli değeri kullanır.
     * Böylece kullanıcı gün içinde ayarı değiştirirse sadece yarına etki eder.
     */
    suspend fun getEffectiveDailyCount(startOfDay: Long): Int
}
