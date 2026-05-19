package com.app.wordlearn.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.QuizQuestion
import com.app.wordlearn.domain.model.QuizSession
import com.app.wordlearn.domain.repository.SessionRepository
import com.app.wordlearn.domain.usecase.BuildDailyQuizUseCase
import com.app.wordlearn.domain.usecase.ProcessAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SettingsRepository

import com.app.wordlearn.domain.util.TtsManager

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val buildDailyQuizUseCase: BuildDailyQuizUseCase,
    private val processAnswerUseCase: ProcessAnswerUseCase,
    private val sessionRepository: SessionRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private var currentIndex = 0
    private var correctCount = 0
    private var sessionId = 0
    private var startTime = 0L

    private var answeredToday = 0
    private var dailyCount = 15
    private var isPracticeMode = false
    private var isProcessingAnswer = false

    fun loadDailyQuiz() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = QuizUiState.Loading

            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = now
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            dailyCount = settingsRepository.getEffectiveDailyCount(calendar.timeInMillis)
            answeredToday = progressRepository.getAnsweredTodayCount(calendar.timeInMillis)
            isPracticeMode = (dailyCount - answeredToday) <= 0

            questions = buildDailyQuizUseCase.execute()

            if (questions.isEmpty()) {
                _uiState.value = QuizUiState.Empty
                return@launch
            }

            // Yeni oturum oluştur
            val session = QuizSession(totalQuestions = questions.size)
            sessionId = sessionRepository.createSession(session).toInt()
            startTime = System.currentTimeMillis()
            currentIndex = 0
            correctCount = 0
            isProcessingAnswer = false

            showCurrentQuestion()
        }
    }

    fun submitAnswer(selectedOption: String) {
        if (isProcessingAnswer) return  // Çift tıklama koruması
        val question = questions.getOrNull(currentIndex) ?: return
        isProcessingAnswer = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = processAnswerUseCase.execute(
                wordId = question.wordId,
                sessionId = sessionId,
                selectedAnswer = selectedOption,
                correctAnswer = question.correctAnswer
            )

            if (result.isCorrect) correctCount++

            _uiState.value = QuizUiState.Feedback(
                isCorrect = result.isCorrect,
                correctAnswer = result.correctAnswer,
                selectedAnswer = selectedOption
            )

            // 1.5 saniye sonra sonraki soruya geç
            delay(1500)
            currentIndex++

            if (currentIndex >= questions.size) {
                finishSession()
            } else {
                showCurrentQuestion()
            }
            isProcessingAnswer = false
        }
    }

    private fun showCurrentQuestion() {
        val question = questions[currentIndex]
        val actualNumber = if (isPracticeMode) currentIndex + 1 else answeredToday + currentIndex + 1
        val dailyTotal = if (isPracticeMode) questions.size else dailyCount

        _uiState.value = QuizUiState.QuizActive(
            currentQuestion = question,
            questionIndex = currentIndex,
            totalQuestions = questions.size,
            score = correctCount,
            actualQuestionNumber = actualNumber,
            dailyTotalQuestions = dailyTotal
        )
    }

    private suspend fun finishSession() {
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()

        // Oturum istatistiklerini güncelle
        val session = QuizSession(
            sessionId = sessionId,
            totalQuestions = questions.size,
            correctCount = correctCount,
            durationSeconds = durationSeconds
        )
        sessionRepository.updateSession(session)

        _uiState.value = QuizUiState.Summary(
            totalQuestions = questions.size,
            correctCount = correctCount,
            duration = durationSeconds
        )
    }
}
