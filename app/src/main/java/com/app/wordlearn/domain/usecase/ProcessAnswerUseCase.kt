package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.AnswerResult
import com.app.wordlearn.domain.model.QuizAnswer
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SessionRepository
import javax.inject.Inject

class ProcessAnswerUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val sessionRepository: SessionRepository
) {
    companion object {
        private const val STREAK_THRESHOLD = 6
        private const val DAY_MS = 86_400_000L
    }

    suspend fun execute(
        wordId: Int,
        sessionId: Int,
        selectedAnswer: String,
        correctAnswer: String
    ): AnswerResult {
        val isCorrect = selectedAnswer == correctAnswer

        // 1. Mevcut ilerlemeyi al
        var currentProgress = progressRepository.getProgress(wordId)
        val isNewProgress = currentProgress == null

        if (currentProgress == null) {
            // Yeni progress kaydı oluştur
            val newProgress = WordProgress(wordId = wordId)
            progressRepository.createProgress(newProgress)
            currentProgress = progressRepository.getProgress(wordId)
                ?: WordProgress(wordId = wordId)
        }

        val now = System.currentTimeMillis()

        // 2-3. Doğru/yanlış cevap mantığı
        val updatedProgress = if (isCorrect) {
            processCorrectAnswer(currentProgress, now)
        } else {
            processWrongAnswer(currentProgress, now)
        }

        // 4. İlerlemeyi güncelle
        progressRepository.updateProgress(updatedProgress)

        // 5. Cevabı kaydet
        val answer = QuizAnswer(
            sessionId = sessionId,
            progressId = currentProgress.progressId,
            isCorrect = isCorrect,
            answeredAt = now
        )
        sessionRepository.saveAnswer(answer)

        // 6. Sonuç döndür
        return AnswerResult(
            isCorrect = isCorrect,
            correctAnswer = correctAnswer,
            newStreak = updatedProgress.correctStreak,
            newStage = updatedProgress.reviewStage
        )
    }

    private fun processCorrectAnswer(progress: WordProgress, now: Long): WordProgress {
        val newStreak = progress.correctStreak + 1

        return if (newStreak >= STREAK_THRESHOLD) {
            // 6 doğru üst üste: stage atlat, streak sıfırla
            val newStage = progress.reviewStage + 1
            val isLearned = newStage >= 5
            val nextReview = if (isLearned) {
                Long.MAX_VALUE
            } else {
                calculateNextReviewDate(newStage, now)
            }

            progress.copy(
                correctStreak = 0,
                reviewStage = newStage,
                totalCorrect = progress.totalCorrect + 1,
                totalAttempts = progress.totalAttempts + 1,
                nextReviewDate = nextReview,
                lastAnsweredDate = now,
                isLearned = isLearned
            )
        } else {
            progress.copy(
                correctStreak = newStreak,
                totalCorrect = progress.totalCorrect + 1,
                totalAttempts = progress.totalAttempts + 1,
                lastAnsweredDate = now
            )
        }
    }

    private fun processWrongAnswer(progress: WordProgress, now: Long): WordProgress {
        // Yanlış cevap: streak ve stage sıfırla, yarın tekrar sor
        return progress.copy(
            correctStreak = 0,
            reviewStage = 0,
            totalAttempts = progress.totalAttempts + 1,
            nextReviewDate = now + DAY_MS,
            lastAnsweredDate = now
        )
    }

    private fun calculateNextReviewDate(stage: Int, now: Long): Long {
        val daysToAdd = when (stage) {
            0 -> 1L
            1 -> 7L
            2 -> 30L
            3 -> 90L
            4 -> 180L
            else -> 365L
        }
        return now + (daysToAdd * DAY_MS)
    }
}
