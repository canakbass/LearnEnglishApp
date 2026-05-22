package com.app.wordlearn.domain.usecase

import android.content.Context
import android.util.Log
import com.app.wordlearn.data.local.dao.WordDao
import com.app.wordlearn.data.local.dao.WordSampleDao
import com.app.wordlearn.data.local.entity.WordSampleEntity
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
 * Sistem kelimeleri ile veritabanını ilk açılışta seed eder + eksik örnek cümleleri doldurur.
 *
 * - Daha önce sadece `Word` tablosunu yazıyordu; `exampleSentence` field'ı parse edilse de
 *   `word_samples` tablosuna kaydedilmiyordu → quiz'de örnek cümle hiç görünmüyordu.
 * - Şimdi seed sırasında hem kelimeyi hem ilk örnek cümlesini yazar.
 * - Daha önce seed olmuş cihazlar için ayrıca `backfillMissingSamples` çağrılır:
 *   örnek cümlesi olmayan sistem kelimeleri için seed'den eklemeyi dener.
 */
class InitializeDatabaseUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val wordDao: WordDao,
    private val wordSampleDao: WordSampleDao,
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun execute() = withContext(Dispatchers.IO) {
        val seedItems = readSeedFromAssets() ?: return@withContext
        if (seedItems.isEmpty()) {
            Log.w(TAG, "Seed file is empty.")
            return@withContext
        }

        val alreadySeeded = wordRepository.getWordCount() > 0

        if (!alreadySeeded) {
            // Fresh install: kelimeleri yaz + örnek cümleleri yaz.
            val words = seedItems.map { it.toDomain() }
            words.chunked(CHUNK_SIZE).forEach { chunk ->
                wordRepository.insertWords(chunk)
            }
            insertSamplesForSeed(seedItems)
            Log.d(TAG, "Seeded ${words.size} system words + samples.")
        } else {
            // Mevcut cihaz: kelime tablosuna dokunma ama eksik sample'ları doldur.
            backfillMissingSamples(seedItems)
        }
    }

    /**
     * Seed'deki her kelime için (system source) engWord → wordId map'i kur,
     * `exampleSentence` dolu olanları `word_samples` tablosuna yaz.
     * Aynı (wordId, sentence) çakışması olursa Room INSERT yine geçer çünkü
     * sampleId autoGenerate, unique constraint yok.
     */
    private suspend fun insertSamplesForSeed(seedItems: List<SystemWordSeedDto>) {
        val seedSamples = seedItems.filter { !it.exampleSentence.isNullOrBlank() }
        if (seedSamples.isEmpty()) return

        val systemWords = wordDao.getSystemWords()
        val engToWordId = systemWords.associate { it.engWord to it.wordId }

        val entities = seedSamples.mapNotNull { dto ->
            val wid = engToWordId[dto.engWord] ?: return@mapNotNull null
            WordSampleEntity(wordId = wid, sentence = dto.exampleSentence!!.trim())
        }
        if (entities.isNotEmpty()) {
            wordSampleDao.insertSamples(entities)
            Log.d(TAG, "Inserted ${entities.size} system word samples.")
        }
    }

    /**
     * Sample'ı olmayan sistem kelimeleri için seed'den ekle.
     * Kullanıcı sample'ları (ya da daha önceden eklenmiş başka sample'lar) ETKİLENMEZ.
     */
    private suspend fun backfillMissingSamples(seedItems: List<SystemWordSeedDto>) {
        val systemWords = wordDao.getSystemWords()
        if (systemWords.isEmpty()) return

        val seedByEng = seedItems
            .filter { !it.exampleSentence.isNullOrBlank() }
            .associate { it.engWord to it.exampleSentence!!.trim() }
        if (seedByEng.isEmpty()) return

        val toInsert = mutableListOf<WordSampleEntity>()
        var skipped = 0
        systemWords.forEach { w ->
            val existing = wordSampleDao.getSamplesByWordId(w.wordId)
            if (existing.isNotEmpty()) {
                skipped++
                return@forEach
            }
            val sentence = seedByEng[w.engWord] ?: return@forEach
            toInsert.add(WordSampleEntity(wordId = w.wordId, sentence = sentence))
        }
        if (toInsert.isNotEmpty()) {
            wordSampleDao.insertSamples(toInsert)
        }
        Log.d(TAG, "Backfilled ${toInsert.size} missing samples (skipped $skipped with existing).")
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
