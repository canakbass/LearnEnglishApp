package com.app.wordlearn.domain.model

data class AnswerResult(
    val isCorrect: Boolean,
    val correctAnswer: String,
    val newStreak: Int,
    val newStage: Int
)
