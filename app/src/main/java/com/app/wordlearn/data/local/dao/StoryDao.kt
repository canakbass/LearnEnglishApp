package com.app.wordlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.wordlearn.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    /** Yedek alma için tek seferlik snapshot — Flow değil suspend. */
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    suspend fun getAll(): List<StoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)
    
    @Query("DELETE FROM stories")
    suspend fun clearAllStories()
}