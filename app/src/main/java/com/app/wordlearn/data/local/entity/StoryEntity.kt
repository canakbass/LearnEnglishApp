package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true)
    val storyId: Long = 0,
    val words: String, // Comma separated words list or JSON
    val storyText: String,
    val imagePath: String?,
    val isLocalImage: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)