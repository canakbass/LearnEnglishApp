package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_answers",
    foreignKeys = [
        ForeignKey(
            entity = QuizSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WordProgressEntity::class,
            parentColumns = ["progressId"],
            childColumns = ["progressId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["sessionId"]),
        androidx.room.Index(value = ["progressId"])
    ]
)
data class QuizAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val answerId: Int = 0,
    val sessionId: Int,
    val progressId: Int,
    val isCorrect: Int = 0,
    val answeredAt: Long = System.currentTimeMillis()
)
