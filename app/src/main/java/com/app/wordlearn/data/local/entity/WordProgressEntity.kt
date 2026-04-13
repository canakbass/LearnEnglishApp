package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_progress",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["wordId"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["wordId"], unique = true)
    ]
)
data class WordProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val progressId: Int = 0,
    val wordId: Int,
    val correctStreak: Int = 0,
    val reviewStage: Int = 0,
    val totalCorrect: Int = 0,
    val totalAttempts: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastAnsweredDate: Long = 0L,
    val isLearned: Int = 0
)
