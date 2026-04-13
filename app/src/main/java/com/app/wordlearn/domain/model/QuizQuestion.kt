package com.app.wordlearn.domain.model

data class QuizQuestion(
    val wordId: Int,
    val questionText: String,
    val correctAnswer: String,
    val options: List<String>
)
