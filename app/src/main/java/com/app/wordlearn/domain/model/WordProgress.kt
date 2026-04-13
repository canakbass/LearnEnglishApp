package com.app.wordlearn.domain.model

data class WordProgress(
    val progressId: Int = 0,
    val wordId: Int,
    val correctStreak: Int = 0,
    val reviewStage: Int = 0,
    val totalCorrect: Int = 0,
    val totalAttempts: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastAnsweredDate: Long = 0L,
    val isLearned: Boolean = false
)
