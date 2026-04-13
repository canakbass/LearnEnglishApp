package com.app.wordlearn.domain.model

data class QuizAnswer(
    val answerId: Int = 0,
    val sessionId: Int,
    val progressId: Int,
    val isCorrect: Boolean,
    val answeredAt: Long = System.currentTimeMillis()
)
