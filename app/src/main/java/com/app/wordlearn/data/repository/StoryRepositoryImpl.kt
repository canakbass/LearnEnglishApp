package com.app.wordlearn.data.repository

import android.content.Context
import com.app.wordlearn.data.local.dao.StoryDao
import com.app.wordlearn.data.local.entity.StoryEntity
import com.app.wordlearn.domain.model.Story
import com.app.wordlearn.domain.repository.StoryRepository
import com.app.wordlearn.domain.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 30_000

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val storyDao: StoryDao,
    @ApplicationContext private val context: Context
) : StoryRepository {

    override fun getAllStories(): Flow<List<Story>> {
        return storyDao.getAllStories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveStory(words: List<String>, storyText: String, imageUrl: String?) {
        withContext(Dispatchers.IO) {
            var finalImagePath: String? = null
            var isLocal = false

            if (!imageUrl.isNullOrBlank()) {
                try {
                    // Download the image with explicit timeouts.
                    // URL.openStream() default timeout = INFINITY → server askıda kalırsa
                    // coroutine sonsuza kadar takılır (SonarQube java:S5527 / S2092).
                    val timestamp = System.currentTimeMillis()
                    val fileName = "story_img_$timestamp.jpg"
                    val file = File(context.filesDir, fileName)

                    val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        requestMethod = "GET"
                    }
                    try {
                        connection.inputStream.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }

                    finalImagePath = file.absolutePath
                    isLocal = true
                } catch (e: Exception) {
                    CrashReporter.reportException(TAG, "Story image download failed", e)
                    // Fallback to URL if download fails
                    finalImagePath = imageUrl
                }
            }

            val entity = StoryEntity(
                words = words.joinToString(","),
                storyText = storyText,
                imagePath = finalImagePath,
                isLocalImage = isLocal
            )
            
            storyDao.insertStory(entity)
        }
    }

    override suspend fun deleteStory(story: Story) {
        withContext(Dispatchers.IO) {
            if (story.isLocalImage && story.imagePath != null) {
                try {
                    val file = File(story.imagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    CrashReporter.reportException(TAG, "Local story image delete failed", e)
                }
            }
            
            val entity = StoryEntity(
                storyId = story.id,
                words = story.words.joinToString(","),
                storyText = story.storyText,
                imagePath = story.imagePath,
                isLocalImage = story.isLocalImage,
                createdAt = story.createdAt
            )
            storyDao.deleteStory(entity)
        }
    }

    override suspend fun clearAllStories() {
        withContext(Dispatchers.IO) {
            storyDao.clearAllStories()
        }
    }

    private fun StoryEntity.toDomainModel(): Story {
        return Story(
            id = storyId,
            words = words.split(",").map { it.trim() },
            storyText = storyText,
            imagePath = imagePath,
            isLocalImage = isLocalImage,
            createdAt = createdAt
        )
    }

    companion object {
        private const val TAG = "StoryRepository"
    }
}