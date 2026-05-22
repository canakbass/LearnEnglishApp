package com.app.wordlearn.domain.usecase

import android.util.Log
import com.app.wordlearn.domain.model.AnswerResult
import com.app.wordlearn.domain.model.QuizAnswer
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SessionRepository
import com.app.wordlearn.domain.util.Constants
import javax.inject.Inject

class ProcessAnswerUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val sessionRepository: SessionRepository
) {

    suspend fun execute(
        wordId: Int,
        sessionId: Int,
        selectedAnswer: String,
        correctAnswer: String
    ): AnswerResult {
        val isCorrect = selectedAnswer == correctAnswer

        // 1. Mevcut ilerlemeyi al
        var currentProgress = progressRepository.getProgress(wordId)

        if (currentProgress == null) {
            // Yeni progress kaydı oluştur
            val newProgress = WordProgress(wordId = wordId)
            progressRepository.createProgress(newProgress)
            currentProgress = progressRepository.getProgress(wordId)
                ?: WordProgress(wordId = wordId)
        }

        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // Kelime bugün zaten cevaplanmışsa hiçbir şey kaydetme:
        // istatistik bozulmaz, score çift sayılmaz, oturum sayacında duplicate olmaz.
        // BuildDailyQuizUseCase'in tek-liste seçim mantığı bu durumu engellemeli;
        // tetiklenirse BuildDailyQuiz'de bug var demektir — log uyarısı bırak.
        if (currentProgress.lastAnsweredDate >= startOfDay) {
            Log.w(
                TAG,
                "Duplicate answer for wordId=$wordId (lastAnsweredDate=${currentProgress.lastAnsweredDate}, " +
                    "startOfDay=$startOfDay). BuildDailyQuiz aynı kelimeyi iki kez sormuş olabilir."
            )
            return AnswerResult(
                isCorrect = isCorrect,
                correctAnswer = correctAnswer,
                newStreak = currentProgress.correctStreak,
                newStage = currentProgress.reviewStage
            )
        }

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

        return if (newStreak >= Constants.STREAK_THRESHOLD) {
            // 6 doğru üst üste: stage atlat, streak sıfırla.
            // Spec'e göre 6 zaman aralığı (1g, 1h, 1ay, 3ay, 6ay, 1y) → 6 stage geçişi sonra öğrenildi.
            val newStage = progress.reviewStage + 1
            val isLearned = newStage >= 6
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
            // Streak devam ediyor ama eşiğe ulaşmadı;
            // nextReviewDate'i de güncelle ki aynı gün tekrar sorulmasın.
            progress.copy(
                correctStreak = newStreak,
                totalCorrect = progress.totalCorrect + 1,
                totalAttempts = progress.totalAttempts + 1,
                nextReviewDate = calculateNextReviewDate(progress.reviewStage, now),
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
            nextReviewDate = now + Constants.DAY_IN_MS,
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
        return now + (daysToAdd * Constants.DAY_IN_MS)
    }

    companion object {
        private const val TAG = "ProcessAnswer"
    }
}
