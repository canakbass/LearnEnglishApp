package com.app.wordlearn.data.repository

import com.app.wordlearn.data.local.dao.QuizAnswerDao
import com.app.wordlearn.data.local.dao.QuizSessionDao
import com.app.wordlearn.data.local.toDomain
import com.app.wordlearn.data.local.toEntity
import com.app.wordlearn.domain.model.QuizAnswer
import com.app.wordlearn.domain.model.QuizSession
import com.app.wordlearn.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val quizSessionDao: QuizSessionDao,
    private val quizAnswerDao: QuizAnswerDao
) : SessionRepository {

    override suspend fun createSession(session: QuizSession): Long =
        quizSessionDao.insertSession(session.toEntity())

    override suspend fun saveAnswer(answer: QuizAnswer) =
        quizAnswerDao.insertAnswer(answer.toEntity())

    override suspend fun getSessionById(id: Int): QuizSession? =
        quizSessionDao.getSessionById(id)?.toDomain()

    override suspend fun getSessionHistory(): List<QuizSession> =
        quizSessionDao.getAllSessions().map { it.toDomain() }

    override suspend fun updateSession(session: QuizSession) =
        quizSessionDao.updateSession(session.toEntity())

    override suspend fun getAnswersBySessionId(sessionId: Int): List<QuizAnswer> =
        quizAnswerDao.getAnswersBySessionId(sessionId).map { it.toDomain() }

    override suspend fun getAllAnswers(): List<QuizAnswer> =
        quizAnswerDao.getAllAnswers().map { it.toDomain() }
}
