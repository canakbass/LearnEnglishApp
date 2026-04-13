package com.app.wordlearn.data.repository

import com.app.wordlearn.data.local.dao.WordDao
import com.app.wordlearn.data.local.dao.WordSampleDao
import com.app.wordlearn.data.local.toDomain
import com.app.wordlearn.data.local.toEntity
import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordSample
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepositoryImpl @Inject constructor(
    private val wordDao: WordDao,
    private val wordSampleDao: WordSampleDao
) : WordRepository {

    override suspend fun getSystemWords(): List<Word> =
        wordDao.getSystemWords().map { it.toDomain() }

    override suspend fun getUserWords(): List<Word> =
        wordDao.getUserWords().map { it.toDomain() }

    override suspend fun getAllWords(): List<Word> =
        wordDao.getAllWords().map { it.toDomain() }

    override suspend fun getWordById(id: Int): Word? =
        wordDao.getWordById(id)?.toDomain()

    override suspend fun searchWords(query: String): List<Word> =
        wordDao.searchWords(query).map { it.toDomain() }

    override suspend fun addUserWord(word: Word): Long =
        wordDao.insertWord(word.copy(source = "user").toEntity())

    override suspend fun getWordSamples(wordId: Int): List<WordSample> =
        wordSampleDao.getSamplesByWordId(wordId).map { it.toDomain() }

    override suspend fun insertWords(words: List<Word>) =
        wordDao.insertWords(words.map { it.toEntity() })

    override suspend fun getWordCount(): Int =
        wordDao.getWordCount()

    override suspend fun getRandomWords(count: Int, excludeId: Int): List<Word> =
        wordDao.getRandomWords(count, excludeId).map { it.toDomain() }
}
