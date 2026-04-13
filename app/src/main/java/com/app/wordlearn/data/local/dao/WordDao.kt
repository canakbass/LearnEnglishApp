package com.app.wordlearn.data.local.dao

import androidx.room.*
import com.app.wordlearn.data.local.entity.WordEntity

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE source = 'system'")
    suspend fun getSystemWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE source = 'user'")
    suspend fun getUserWords(): List<WordEntity>

    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE wordId = :id")
    suspend fun getWordById(id: Int): WordEntity?

    @Query("SELECT * FROM words WHERE engWord LIKE '%' || :query || '%' OR turWord LIKE '%' || :query || '%'")
    suspend fun searchWords(query: String): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Query("SELECT * FROM words WHERE wordId != :excludeId ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomWords(count: Int, excludeId: Int): List<WordEntity>

    @Delete
    suspend fun deleteWord(word: WordEntity)
}
