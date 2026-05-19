package com.app.wordlearn.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.app.wordlearn.data.local.AppDatabase
import com.app.wordlearn.data.local.dao.QuizAnswerDao
import com.app.wordlearn.data.local.dao.QuizSessionDao
import com.app.wordlearn.data.local.dao.SettingsDao
import com.app.wordlearn.data.local.dao.StoryDao
import com.app.wordlearn.data.local.dao.WordDao
import com.app.wordlearn.data.local.dao.WordProgressDao
import com.app.wordlearn.data.local.dao.WordSampleDao
import com.app.wordlearn.data.local.entity.QuizAnswerEntity
import com.app.wordlearn.data.local.entity.QuizSessionEntity
import com.app.wordlearn.data.local.entity.SettingsEntity
import com.app.wordlearn.data.local.entity.StoryEntity
import com.app.wordlearn.data.local.entity.WordEntity
import com.app.wordlearn.data.local.entity.WordProgressEntity
import com.app.wordlearn.data.local.entity.WordSampleEntity
import com.app.wordlearn.domain.model.BackupData
import com.app.wordlearn.domain.model.QuizAnswerDto
import com.app.wordlearn.domain.model.QuizSessionDto
import com.app.wordlearn.domain.model.SettingsDto
import com.app.wordlearn.domain.model.StoryDto
import com.app.wordlearn.domain.model.WordDto
import com.app.wordlearn.domain.model.WordProgressDto
import com.app.wordlearn.domain.model.WordSampleDto
import com.app.wordlearn.domain.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı verilerini ZIP arşivi olarak dışa/içe aktarır.
 *
 *   wordlearn-backup.zip
 *   ├── data.json        — tüm tablo içerikleri (örnek cümleler, ilerleme, vs)
 *   └── images/          — kullanıcı kelimelerinin ve hikayelerin resimleri
 *
 * - Sistem kelimeleri (source = "system") yedeklenmez; yeni cihazda seed JSON'dan gelir.
 * - Eşleştirme [engWord] üzerinden yapılır; bu sayede başka bir cihazda
 *   farklı `wordId`'ler olsa bile ilerleme kayıtları doğru kelimeye bağlanır.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val wordDao: WordDao,
    private val wordSampleDao: WordSampleDao,
    private val wordProgressDao: WordProgressDao,
    private val quizSessionDao: QuizSessionDao,
    private val quizAnswerDao: QuizAnswerDao,
    private val settingsDao: SettingsDao,
    private val storyDao: StoryDao,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Verilen Uri (SAF document) içine ZIP yazar. */
    suspend fun exportTo(destination: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val userWords = wordDao.getUserWords()
            val allWordsById = wordDao.getAllWords().associateBy { it.wordId }
            val allSamples = wordSampleDao.getAll()
            val allProgress = wordProgressDao.getAllProgress()
            val allSessions = quizSessionDao.getAllSessions()
            val allAnswers = quizAnswerDao.getAllAnswers()
            val storedSettings = settingsDao.getSettings()
            val allStories = storyDao.getAll()

            val pictureMap = mutableMapOf<Int, String>()  // wordId → filename in zip
            val storyImageMap = mutableMapOf<Long, String>()

            val wordDtos = userWords.map { w ->
                val pictureFile = w.picturePath?.let { path ->
                    fileNameForWord(w).also { pictureMap[w.wordId] = it; }
                        .takeIf { resolveExistingInput(path) != null }
                }
                WordDto(
                    originalWordId = w.wordId,
                    engWord = w.engWord,
                    turWord = w.turWord,
                    pictureFileName = pictureFile,
                    audioFileName = null,
                    level = w.level,
                    category = w.category,
                    createdAt = w.createdAt
                )
            }

            val sampleDtos = allSamples.mapNotNull { s ->
                val engWord = allWordsById[s.wordId]?.engWord ?: return@mapNotNull null
                WordSampleDto(engWord = engWord, sentence = s.sentence)
            }

            val progressDtos = allProgress.mapNotNull { p ->
                val engWord = allWordsById[p.wordId]?.engWord ?: return@mapNotNull null
                WordProgressDto(
                    engWord = engWord,
                    correctStreak = p.correctStreak,
                    reviewStage = p.reviewStage,
                    totalCorrect = p.totalCorrect,
                    totalAttempts = p.totalAttempts,
                    nextReviewDate = p.nextReviewDate,
                    lastAnsweredDate = p.lastAnsweredDate,
                    isLearned = p.isLearned == 1
                )
            }

            val progressIdToEng: Map<Int, String> = allProgress.mapNotNull { p ->
                val eng = allWordsById[p.wordId]?.engWord ?: return@mapNotNull null
                p.progressId to eng
            }.toMap()

            val sessionDtos = allSessions.map {
                QuizSessionDto(
                    originalSessionId = it.sessionId,
                    sessionDate = it.sessionDate,
                    totalQuestions = it.totalQuestions,
                    correctCount = it.correctCount,
                    durationSeconds = it.durationSeconds
                )
            }
            val answerDtos = allAnswers.mapNotNull {
                val eng = progressIdToEng[it.progressId] ?: return@mapNotNull null
                QuizAnswerDto(
                    originalSessionId = it.sessionId,
                    engWord = eng,
                    isCorrect = it.isCorrect == 1,
                    answeredAt = it.answeredAt
                )
            }

            val settingsDto = storedSettings?.let {
                SettingsDto(
                    displayName = it.displayName,
                    dailyNewWordCount = it.dailyNewWordCount,
                    userLevel = it.userLevel,
                    updatedAt = it.updatedAt
                )
            }

            val storyDtos = allStories.map { s ->
                val imageFile = s.imagePath?.takeIf { s.isLocalImage }?.let { path ->
                    fileNameForStory(s).also { storyImageMap[s.storyId] = it }
                        .takeIf { resolveExistingInput(path) != null }
                }
                StoryDto(
                    words = s.words,
                    storyText = s.storyText,
                    imageFileName = imageFile,
                    isLocalImage = s.isLocalImage,
                    createdAt = s.createdAt
                )
            }

            val backup = BackupData(
                appVersionName = appVersionName(),
                createdAt = System.currentTimeMillis(),
                userWords = wordDtos,
                wordSamples = sampleDtos,
                progress = progressDtos,
                quizSessions = sessionDtos,
                quizAnswers = answerDtos,
                settings = settingsDto,
                stories = storyDtos
            )

            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("Çıktı dosyası açılamadı")

            ZipOutputStream(output.buffered()).use { zip ->
                // data.json
                zip.putNextEntry(ZipEntry(DATA_JSON))
                zip.write(json.encodeToString(backup).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // images/
                userWords.forEach { w ->
                    val path = w.picturePath ?: return@forEach
                    val filename = pictureMap[w.wordId] ?: return@forEach
                    resolveExistingInput(path)?.use { input ->
                        zip.putNextEntry(ZipEntry("$IMAGES_DIR/$filename"))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
                allStories.forEach { s ->
                    if (!s.isLocalImage) return@forEach
                    val path = s.imagePath ?: return@forEach
                    val filename = storyImageMap[s.storyId] ?: return@forEach
                    resolveExistingInput(path)?.use { input ->
                        zip.putNextEntry(ZipEntry("$IMAGES_DIR/$filename"))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
            }

            backup.userWords.size
        }.onFailure { CrashReporter.reportException(TAG, "Export failed", it) }
    }

    /** Verilen Uri'den ZIP'i okur, mevcut kullanıcı verisini değiştirir. */
    suspend fun importFrom(source: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. ZIP'i geçici bir klasöre aç.
            val extractDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
            var backup: BackupData? = null
            context.contentResolver.openInputStream(source)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == DATA_JSON) {
                            val text = zip.readBytes().toString(Charsets.UTF_8)
                            backup = json.decodeFromString<BackupData>(text)
                        } else if (entry.name.startsWith("$IMAGES_DIR/") && !entry.isDirectory) {
                            val outFile = File(extractDir, entry.name.removePrefix("$IMAGES_DIR/"))
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Yedek dosyası açılamadı")

            val data = backup ?: error("Yedek dosyasında data.json bulunamadı")
            require(data.schemaVersion == BackupData.SCHEMA_VERSION) {
                "Bu yedek sürümü (${data.schemaVersion}) desteklenmiyor"
            }

            // 2. Resimleri kalıcı klasöre taşı.
            val imagesDir = File(context.filesDir, "user_images").apply { mkdirs() }
            val extractedImages = extractDir.listFiles().orEmpty()
            val imagePathMap: Map<String, String> = extractedImages.associate { f ->
                val dest = File(imagesDir, "${System.currentTimeMillis()}_${f.name}")
                f.copyTo(dest, overwrite = true)
                f.name to dest.absolutePath
            }
            extractDir.deleteRecursively()

            // 3. Transaction içinde yeniden inşa et.
            appDatabase.withTransaction {
                quizAnswerDao.deleteAll()
                quizSessionDao.deleteAll()
                wordProgressDao.deleteAll()
                wordSampleDao.deleteAll()
                storyDao.clearAllStories()
                settingsDao.deleteAll()
                wordDao.deleteAllUserWords()

                // User words insert + engWord → newWordId mapping
                val engToNewId = mutableMapOf<String, Int>()

                // önce DB'de var olan sistem kelimelerinin map'ini al
                wordDao.getAllWords().forEach { engToNewId[it.engWord] = it.wordId }

                data.userWords.forEach { dto ->
                    val pic = dto.pictureFileName?.let { imagePathMap[it] }
                    val newId = wordDao.insertWord(
                        WordEntity(
                            engWord = dto.engWord,
                            turWord = dto.turWord,
                            picturePath = pic,
                            audioPath = null,
                            level = dto.level,
                            category = dto.category,
                            source = "user",
                            createdAt = dto.createdAt
                        )
                    ).toInt()
                    engToNewId[dto.engWord] = newId
                }

                // Samples
                val sampleEntities = data.wordSamples.mapNotNull {
                    val wid = engToNewId[it.engWord] ?: return@mapNotNull null
                    WordSampleEntity(wordId = wid, sentence = it.sentence)
                }
                if (sampleEntities.isNotEmpty()) wordSampleDao.insertSamples(sampleEntities)

                // Progress restore: engWord → progressId map'i kurmak için
                // wordId → engWord ters map kullan (doğrudan, O(1) lookup)
                val progressByEng = mutableMapOf<String, Int>()
                val newIdToEng: Map<Int, String> = engToNewId.entries.associate { (eng, wid) -> wid to eng }

                data.progress.forEach { dto ->
                    val wid = engToNewId[dto.engWord] ?: return@forEach
                    wordProgressDao.insertProgress(
                        WordProgressEntity(
                            wordId = wid,
                            correctStreak = dto.correctStreak,
                            reviewStage = dto.reviewStage,
                            totalCorrect = dto.totalCorrect,
                            totalAttempts = dto.totalAttempts,
                            nextReviewDate = dto.nextReviewDate,
                            lastAnsweredDate = dto.lastAnsweredDate,
                            isLearned = if (dto.isLearned) 1 else 0
                        )
                    )
                }
                // Yeni atanan progressId'leri oku ve eng map'ini kur
                wordProgressDao.getAllProgress().forEach { p ->
                    val eng = newIdToEng[p.wordId]
                    if (eng != null) progressByEng[eng] = p.progressId
                }

                // Quiz sessions: insert tek tek ki yeni sessionId mapping çıkartabilelim
                val oldToNewSession = mutableMapOf<Int, Int>()
                data.quizSessions.forEach { s ->
                    val newId = quizSessionDao.insertSession(
                        QuizSessionEntity(
                            sessionDate = s.sessionDate,
                            totalQuestions = s.totalQuestions,
                            correctCount = s.correctCount,
                            durationSeconds = s.durationSeconds
                        )
                    ).toInt()
                    oldToNewSession[s.originalSessionId] = newId
                }

                // Quiz answers
                data.quizAnswers.forEach { a ->
                    val sid = oldToNewSession[a.originalSessionId] ?: return@forEach
                    val pid = progressByEng[a.engWord] ?: return@forEach
                    quizAnswerDao.insertAnswer(
                        QuizAnswerEntity(
                            sessionId = sid,
                            progressId = pid,
                            isCorrect = if (a.isCorrect) 1 else 0,
                            answeredAt = a.answeredAt
                        )
                    )
                }

                // Settings
                data.settings?.let {
                    settingsDao.insertSettings(
                        SettingsEntity(
                            id = 1,
                            firebaseUid = "",
                            displayName = it.displayName,
                            dailyNewWordCount = it.dailyNewWordCount,
                            userLevel = it.userLevel,
                            updatedAt = it.updatedAt
                        )
                    )
                }

                // Stories
                val storyEntities = data.stories.map { s ->
                    val img = s.imageFileName?.let { imagePathMap[it] }
                    StoryEntity(
                        words = s.words,
                        storyText = s.storyText,
                        imagePath = img ?: s.imageFileName?.takeIf { !s.isLocalImage },
                        isLocalImage = s.isLocalImage && img != null,
                        createdAt = s.createdAt
                    )
                }
                if (storyEntities.isNotEmpty()) storyDao.insertAll(storyEntities)
            }

            data.userWords.size
        }.onFailure { CrashReporter.reportException(TAG, "Import failed", it) }
    }

    // ---------- helpers ----------
    private fun resolveExistingInput(path: String): InputStream? {
        val file = File(path)
        if (file.isFile) return FileInputStream(file)
        return runCatching { context.contentResolver.openInputStream(Uri.parse(path)) }.getOrNull()
    }

    private fun fileNameForWord(w: WordEntity): String {
        val ext = w.picturePath?.substringAfterLast('.', missingDelimiterValue = "jpg")?.take(4) ?: "jpg"
        return "word_${w.wordId}_${sanitize(w.engWord)}.$ext"
    }

    private fun fileNameForStory(s: StoryEntity): String {
        val ext = s.imagePath?.substringAfterLast('.', missingDelimiterValue = "jpg")?.take(4) ?: "jpg"
        return "story_${s.storyId}.$ext"
    }

    private fun sanitize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40)

    private fun appVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (_: Exception) {
        ""
    }

    companion object {
        private const val TAG = "BackupRepository"
        private const val DATA_JSON = "data.json"
        private const val IMAGES_DIR = "images"
    }
}
