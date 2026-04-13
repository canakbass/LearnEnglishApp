package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.AnalyticsData
import com.app.wordlearn.domain.model.DailyStats
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SessionRepository
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

class GetAnalyticsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val progressRepository: ProgressRepository,
    private val wordRepository: WordRepository
) {
    companion object {
        private const val DAY_MS = 86_400_000L
    }

    suspend fun execute(): AnalyticsData {
        val allProgress = progressRepository.getAllProgress()
        val allWords = wordRepository.getAllWords()
        val allAnswers = sessionRepository.getAllAnswers()
        val sessions = sessionRepository.getSessionHistory()

        // Kelime ID → Word map
        val wordMap = allWords.associateBy { it.wordId }

        // Kategori bazlı başarı oranı
        val categoryStats = mutableMapOf<String, Pair<Int, Int>>() // correct, total
        allProgress.forEach { progress ->
            val word = wordMap[progress.wordId] ?: return@forEach
            val category = word.category
            val current = categoryStats.getOrDefault(category, Pair(0, 0))
            categoryStats[category] = Pair(
                current.first + progress.totalCorrect,
                current.second + progress.totalAttempts
            )
        }
        val categorySuccessRates = categoryStats.mapValues { (_, value) ->
            if (value.second > 0) value.first.toFloat() / value.second * 100 else 0f
        }

        // Seviye bazlı başarı oranı
        val levelStats = mutableMapOf<String, Pair<Int, Int>>()
        allProgress.forEach { progress ->
            val word = wordMap[progress.wordId] ?: return@forEach
            val level = word.level
            val current = levelStats.getOrDefault(level, Pair(0, 0))
            levelStats[level] = Pair(
                current.first + progress.totalCorrect,
                current.second + progress.totalAttempts
            )
        }
        val levelSuccessRates = levelStats.mapValues { (_, value) ->
            if (value.second > 0) value.first.toFloat() / value.second * 100 else 0f
        }

        // Günlük trend (son 30 gün)
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * DAY_MS)
        val dailyTrend = allAnswers
            .filter { it.answeredAt >= thirtyDaysAgo }
            .groupBy { it.answeredAt / DAY_MS }
            .map { (dayKey, answers) ->
                DailyStats(
                    date = dayKey * DAY_MS,
                    correctCount = answers.count { it.isCorrect },
                    totalCount = answers.size
                )
            }
            .sortedBy { it.date }

        // Genel istatistikler
        val totalLearnedWords = allProgress.count { it.isLearned }
        val totalCorrect = allProgress.sumOf { it.totalCorrect }
        val totalAttempts = allProgress.sumOf { it.totalAttempts }
        val averageSuccess = if (totalAttempts > 0) {
            totalCorrect.toFloat() / totalAttempts * 100
        } else 0f

        // En uzun doğru seri
        val longestStreak = allProgress.maxOfOrNull { it.correctStreak } ?: 0

        return AnalyticsData(
            categorySuccessRates = categorySuccessRates,
            levelSuccessRates = levelSuccessRates,
            dailyTrend = dailyTrend,
            totalLearnedWords = totalLearnedWords,
            totalQuestions = totalAttempts,
            averageSuccess = averageSuccess,
            longestStreak = longestStreak
        )
    }
}
