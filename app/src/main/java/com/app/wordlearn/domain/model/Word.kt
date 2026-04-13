package com.app.wordlearn.domain.model

data class Word(
    val wordId: Int = 0,
    val engWord: String,
    val turWord: String,
    val picturePath: String? = null,
    val audioPath: String? = null,
    val level: String = "Orta",
    val category: String = "Genel",
    val source: String = "system",
    val createdAt: Long = System.currentTimeMillis()
)
