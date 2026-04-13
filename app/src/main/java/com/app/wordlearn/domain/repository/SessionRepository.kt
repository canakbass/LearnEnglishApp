package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.QuizAnswer
import com.app.wordlearn.domain.model.QuizSession

interface SessionRepository {
    suspend fun createSession(session: QuizSession): Long
    suspend fun saveAnswer(answer: QuizAnswer)
    suspend fun getSessionById(id: Int): QuizSession?
    suspend fun getSessionHistory(): List<QuizSession>
    suspend fun updateSession(session: QuizSession)
    suspend fun getAnswersBySessionId(sessionId: Int): List<QuizAnswer>
    suspend fun getAllAnswers(): List<QuizAnswer>
}
