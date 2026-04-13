package com.app.wordlearn.data.repository

import com.app.wordlearn.data.local.dao.WordProgressDao
import com.app.wordlearn.data.local.toDomain
import com.app.wordlearn.data.local.toEntity
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val wordProgressDao: WordProgressDao
) : ProgressRepository {

    override suspend fun getProgress(wordId: Int): WordProgress? =
        wordProgressDao.getProgressByWordId(wordId)?.toDomain()

    override suspend fun getDueWords(today: Long): List<WordProgress> =
        wordProgressDao.getDueWords(today).map { it.toDomain() }

    override suspend fun getNewWords(count: Int): List<WordProgress> =
        wordProgressDao.getNewWords(count).map { it.toDomain() }

    override suspend fun updateProgress(progress: WordProgress) =
        wordProgressDao.updateProgress(progress.toEntity())

    override suspend fun getLearnedWords(): List<WordProgress> =
        wordProgressDao.getLearnedWords().map { it.toDomain() }

    override suspend fun markAsLearned(wordId: Int) =
        wordProgressDao.markAsLearned(wordId)

    override suspend fun createProgress(progress: WordProgress) =
        wordProgressDao.insertProgress(progress.toEntity()).let { }

    override suspend fun getAllProgress(): List<WordProgress> =
        wordProgressDao.getAllProgress().map { it.toDomain() }

    override suspend fun getProgressCount(): Int =
        wordProgressDao.getProgressCount()
}
