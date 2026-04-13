package com.app.wordlearn.data.repository

import com.app.wordlearn.data.local.dao.SettingsDao
import com.app.wordlearn.data.local.toDomain
import com.app.wordlearn.data.local.toEntity
import com.app.wordlearn.domain.model.Settings
import com.app.wordlearn.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override suspend fun getSettings(): Settings {
        val entity = settingsDao.getSettings()
        return entity?.toDomain() ?: Settings().also {
            settingsDao.insertSettings(it.toEntity())
        }
    }

    override suspend fun getDailyNewWordCount(): Int =
        getSettings().dailyNewWordCount

    override suspend fun setDailyNewWordCount(count: Int) {
        val settings = getSettings().copy(
            dailyNewWordCount = count,
            updatedAt = System.currentTimeMillis()
        )
        settingsDao.insertSettings(settings.toEntity())
    }

    override suspend fun getUserLevel(): String =
        getSettings().userLevel

    override suspend fun setUserLevel(level: String) {
        val settings = getSettings().copy(
            userLevel = level,
            updatedAt = System.currentTimeMillis()
        )
        settingsDao.insertSettings(settings.toEntity())
    }

    override suspend fun saveSettings(settings: Settings) =
        settingsDao.insertSettings(settings.toEntity())
}
