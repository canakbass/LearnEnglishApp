package com.app.wordlearn.domain.model

data class Settings(
    val id: Int = 1,
    val firebaseUid: String = "",
    val displayName: String = "",
    val dailyNewWordCount: Int = 10,
    val userLevel: String = "Başlangıç",
    val updatedAt: Long = System.currentTimeMillis()
)
