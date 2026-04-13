package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
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
