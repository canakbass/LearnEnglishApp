package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordSample

interface WordRepository {
    suspend fun getSystemWords(): List<Word>
    suspend fun getUserWords(): List<Word>
    suspend fun getAllWords(): List<Word>
    suspend fun getWordById(id: Int): Word?
    suspend fun searchWords(query: String): List<Word>
    suspend fun addUserWord(word: Word): Long
    suspend fun getWordSamples(wordId: Int): List<WordSample>
    suspend fun insertWords(words: List<Word>)
    suspend fun getWordCount(): Int
    suspend fun getRandomWords(count: Int, excludeId: Int): List<Word>
}
