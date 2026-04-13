package com.app.wordlearn.data.local.dao

import androidx.room.*
import com.app.wordlearn.data.local.entity.WordSampleEntity

@Dao
interface WordSampleDao {
    @Query("SELECT * FROM word_samples WHERE wordId = :wordId")
    suspend fun getSamplesByWordId(wordId: Int): List<WordSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: WordSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<WordSampleEntity>)
}
