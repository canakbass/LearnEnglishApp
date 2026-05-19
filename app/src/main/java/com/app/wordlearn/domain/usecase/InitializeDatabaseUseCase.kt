package com.app.wordlearn.domain.usecase

import android.content.Context
import android.util.Log
import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.repository.WordRepository
import com.app.wordlearn.domain.util.Constants
import com.app.wordlearn.domain.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Sistem kelimeleri ile veritabanını ilk açılışta seed eder.
 *
 * - Daha önce ham SQL dosyasından regex ile parse ediyordu; tek tırnak içeren
 *   değerlerde kırılabilir + örnek cümleyi atıyor + büyük dosyayı tek seferde
 *   belleğe alıyordu.
 * - Şimdi `system_words_seed.json` üzerinden kotlinx-serialization ile
 *   güvenli ve örnek cümleleri de içeren biçimde okur.
 */
class InitializeDatabaseUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun execute() = withContext(Dispatchers.IO) {
        if (wordRepository.getWordCount() > 0) {
            Log.d(TAG, "Database already seeded; skipping.")
            return@withContext
        }

        val seedItems = readSeedFromAssets() ?: return@withContext
        if (seedItems.isEmpty()) {
            Log.w(TAG, "Seed file is empty.")
            return@withContext
        }

        val words = seedItems.map { it.toDomain() }
        words.chunked(CHUNK_SIZE).forEach { chunk ->
            wordRepository.insertWords(chunk)
        }
        Log.d(TAG, "Seeded ${words.size} system words.")
    }

    private fun readSeedFromAssets(): List<SystemWordSeedDto>? = try {
        context.assets.open(Constants.DATABASE_SEED_JSON).use { input ->
            val text = input.bufferedReader(Charsets.UTF_8).readText()
            json.decodeFromString<List<SystemWordSeedDto>>(text)
        }
    } catch (e: Exception) {
        CrashReporter.reportException(TAG, "Failed to read seed JSON", e)
        null
    }

    private fun SystemWordSeedDto.toDomain() = Word(
        engWord = engWord,
        turWord = turWord,
        level = level,
        category = category,
        source = Constants.SYSTEM_SOURCE
    )

    @Serializable
    private data class SystemWordSeedDto(
        val engWord: String,
        val turWord: String,
        val level: String = Constants.DEFAULT_WORD_LEVEL,
        val category: String = Constants.DEFAULT_CATEGORY,
        val exampleSentence: String? = null
    )

    companion object {
        private const val TAG = "InitializeDatabase"
        private const val CHUNK_SIZE = 500
    }
}
