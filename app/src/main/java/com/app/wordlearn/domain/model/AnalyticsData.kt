package com.app.wordlearn.domain.model

data class AnalyticsData(
    val categorySuccessRates: Map<String, Float>,
    val levelSuccessRates: Map<String, Float>,
    val dailyTrend: List<DailyStats>,
    val totalLearnedWords: Int,
    val totalQuestions: Int,
    val averageSuccess: Float,
    val longestStreak: Int
)

data class DailyStats(
    val date: Long,
    val correctCount: Int,
    val totalCount: Int
)
