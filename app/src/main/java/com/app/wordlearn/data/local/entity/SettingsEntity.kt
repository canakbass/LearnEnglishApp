package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val firebaseUid: String = "",
    val displayName: String = "",
    val dailyNewWordCount: Int = 10,
    val userLevel: String = "Başlangıç",
    val updatedAt: Long = System.currentTimeMillis()
)
