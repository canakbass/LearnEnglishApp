package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getAllStories(): Flow<List<Story>>
    suspend fun saveStory(words: List<String>, storyText: String, imageUrl: String?)
    suspend fun deleteStory(story: Story)
    suspend fun clearAllStories()
}