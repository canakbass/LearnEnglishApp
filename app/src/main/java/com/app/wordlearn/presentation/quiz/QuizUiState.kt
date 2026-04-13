package com.app.wordlearn.presentation.quiz

import com.app.wordlearn.domain.model.QuizQuestion

sealed class QuizUiState {
    data object Loading : QuizUiState()
    data class QuizActive(
        val currentQuestion: QuizQuestion,
        val questionIndex: Int,
        val totalQuestions: Int,
        val score: Int
    ) : QuizUiState()
    data class Feedback(
        val isCorrect: Boolean,
        val correctAnswer: String,
        val selectedAnswer: String
    ) : QuizUiState()
    data class Summary(
        val totalQuestions: Int,
        val correctCount: Int,
        val duration: Int
    ) : QuizUiState()
    data object Empty : QuizUiState()
}
