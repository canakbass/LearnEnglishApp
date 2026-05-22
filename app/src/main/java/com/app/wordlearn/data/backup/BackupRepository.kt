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
import com.app.wordlearn.domain.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
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
                // BUG #11 fix: side-effect'i takeIf öncesine almıştık — yalnızca dosya gerçekten okunabiliyorsa
                // pictureMap'e ekle. Aksi halde ZIP'e boş entry yazma denemesi olur.
                val pictureFile = w.picturePath?.let { path ->
                    if (resolveExistingInput(path) != null) {
                        val name = fileNameForWord(w)
                        pictureMap[w.wordId] = name
                        name
                    } else null
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

            // engWord başına dedup: aynı engWord birden fazla wordId'ye (sistem + user) bağlıysa,
            // restore'da REPLACE ile birbirini ezecek progress'lerin en bilgili olanını koru.
            // Öncelik: totalAttempts > 0 olanlar; eşitlikte totalCorrect yüksek olan.
            val progressDtos = allProgress.mapNotNull { p ->
                val engWord = allWordsById[p.wordId]?.engWord ?: return@mapNotNull null
                engWord to WordProgressDto(
                    engWord = engWord,
                    correctStreak = p.correctStreak,
                    reviewStage = p.reviewStage,
                    totalCorrect = p.totalCorrect,
                    totalAttempts = p.totalAttempts,
                    nextReviewDate = p.nextReviewDate,
                    lastAnsweredDate = p.lastAnsweredDate,
                    isLearned = p.isLearned == 1,
                    lastShownDate = p.lastShownDate
                )
            }
                .groupBy({ it.first }, { it.second })
                .mapNotNull { (_, candidates) -> pickRichestProgress(candidates) }

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
                    if (resolveExistingInput(path) != null) {
                        val name = fileNameForStory(s)
                        storyImageMap[s.storyId] = name
                        name
                    } else null
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

            val imgStats = ZipOutputStream(output.buffered()).use { zip ->
                writeJsonEntry(zip, backup)
                writeImageEntries(zip, userWords, pictureMap, allStories, storyImageMap)
            }
            CrashReporter.log(
                TAG,
                "Export done: words=${backup.userWords.size} " +
                    "imagesWritten=${imgStats.written} imagesSkipped=${imgStats.skipped}"
            )

            backup.userWords.size
        }.onFailure { CrashReporter.reportException(TAG, "Export failed", it) }
    }

    private fun writeJsonEntry(zip: ZipOutputStream, backup: BackupData) {
        zip.putNextEntry(ZipEntry(DATA_JSON))
        zip.write(json.encodeToString(backup).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /** Hem user word hem story resimlerini ZIP'e yazar; (yazılan, atlanan) sayar. */
    private fun writeImageEntries(
        zip: ZipOutputStream,
        userWords: List<WordEntity>,
        pictureMap: Map<Int, String>,
        allStories: List<StoryEntity>,
        storyImageMap: Map<Long, String>
    ): ImageWriteStats {
        var written = 0
        var skipped = 0
        userWords.forEach { w ->
            val path = w.picturePath ?: return@forEach
            val filename = pictureMap[w.wordId] ?: return@forEach
            if (copyToZipEntry(zip, path, filename)) written++ else {
                skipped++
                CrashReporter.log(TAG, "Export: word ${w.wordId} '$path' okunamadı, atlandı")
            }
        }
        allStories.forEach { s ->
            if (!s.isLocalImage) return@forEach
            val path = s.imagePath ?: return@forEach
            val filename = storyImageMap[s.storyId] ?: return@forEach
            if (copyToZipEntry(zip, path, filename)) written++
        }
        return ImageWriteStats(written, skipped)
    }

    private fun copyToZipEntry(zip: ZipOutputStream, srcPath: String, zipFilename: String): Boolean {
        val input = resolveExistingInput(srcPath) ?: return false
        input.use {
            zip.putNextEntry(ZipEntry("$IMAGES_DIR/$zipFilename"))
            it.copyTo(zip)
            zip.closeEntry()
        }
        return true
    }

    private data class ImageWriteStats(val written: Int, val skipped: Int)

    /** Restore istatistikleri — UI'ya geri bildirim için. */
    data class ImportStats(
        val wordCount: Int,
        val progressRestored: Int,
        val progressDropped: Int,
        val answersRestored: Int,
        val answersDropped: Int
    )

    /** Verilen Uri'den ZIP'i okur, mevcut kullanıcı verisini değiştirir. */
    suspend fun importFrom(source: Uri): Result<ImportStats> = withContext(Dispatchers.IO) {
        runCatching {
            // 1) ZIP'i geçici klasöre aç → BackupData + (filename → tempFile) map
            val extracted = extractBackupZip(source)
            val data = extracted.data
            require(data.schemaVersion == BackupData.SCHEMA_VERSION) {
                "Bu yedek sürümü (${data.schemaVersion}) desteklenmiyor"
            }

            // 2) Resimleri kalıcı user_images klasörüne taşı
            val imagePathMap = persistExtractedImages(extracted.extractDir)

            // 3) Transaction içinde DB'yi yeniden inşa et
            val restore = applyRestoreTransaction(data, imagePathMap)

            settingsRepository.invalidateDailyCountCache()

            val stats = ImportStats(
                wordCount = data.userWords.size,
                progressRestored = restore.progressRestored,
                progressDropped = restore.progressDropped,
                answersRestored = restore.answersRestored,
                answersDropped = restore.answersDropped
            )
            CrashReporter.log(
                TAG,
                "Import done: deletedUserWords=${restore.deletedUserWords} " +
                    "newUserWords=${stats.wordCount} progress=${stats.progressRestored} " +
                    "(dropped=${stats.progressDropped}) answers=${stats.answersRestored} " +
                    "(dropped=${stats.answersDropped})"
            )
            stats
        }.onFailure { CrashReporter.reportException(TAG, "Import failed", it) }
    }

    /** ZIP'i cache'e aç; data.json'u parse et, görselleri geçici dizine yaz. */
    private fun extractBackupZip(source: Uri): ExtractedBackup {
        val extractDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
        var backup: BackupData? = null
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    backup = readEntry(zip, entry, extractDir) ?: backup
                    zip.closeEntry()
                }
            }
        } ?: error("Yedek dosyası açılamadı")
        val data = backup ?: error("Yedek dosyasında data.json bulunamadı")
        return ExtractedBackup(data, extractDir)
    }

    /** Tek bir zip entry'yi işler; data.json ise parse'lı sonuç döner, yoksa null. */
    private fun readEntry(zip: ZipInputStream, entry: ZipEntry, extractDir: File): BackupData? {
        if (entry.name == DATA_JSON) {
            val text = zip.readBytes().toString(Charsets.UTF_8)
            return json.decodeFromString<BackupData>(text)
        }
        if (entry.name.startsWith("$IMAGES_DIR/") && !entry.isDirectory) {
            val outFile = File(extractDir, entry.name.removePrefix("$IMAGES_DIR/"))
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { out -> zip.copyTo(out) }
        }
        return null
    }

    /** Geçici klasördeki resimleri user_images'a kopyalar. (zipFileName → kalıcı path) */
    private fun persistExtractedImages(extractDir: File): Map<String, String> {
        val imagesDir = File(context.filesDir, "user_images").apply { mkdirs() }
        val extractedImages = extractDir.listFiles().orEmpty().filter { it.isFile }
        var copied = 0
        var failed = 0
        val map: Map<String, String> = extractedImages.mapNotNull { f ->
            runCatching {
                val dest = File(imagesDir, "${System.currentTimeMillis()}_${f.name}")
                f.copyTo(dest, overwrite = true)
                if (dest.length() <= 0L) error("0 byte dest file")
                copied++
                f.name to dest.absolutePath
            }.onFailure {
                failed++
                CrashReporter.log(TAG, "Image kopyalama hatası ${f.name}: ${it.message}")
            }.getOrNull()
        }.toMap()
        extractDir.deleteRecursively()
        CrashReporter.log(TAG, "Extracted images: ${extractedImages.size}, copied=$copied, failed=$failed")
        return map
    }

    private data class ExtractedBackup(val data: BackupData, val extractDir: File)

    private data class RestoreCounts(
        val deletedUserWords: Int,
        val progressRestored: Int,
        val progressDropped: Int,
        val answersRestored: Int,
        val answersDropped: Int
    )

    /** Tüm restore işlemini tek transaction içinde yapar, sayım döner. */
    private suspend fun applyRestoreTransaction(
        data: BackupData,
        imagePathMap: Map<String, String>
    ): RestoreCounts {
        var progressRestored = 0
        var progressDropped = 0
        var answersRestored = 0
        var answersDropped = 0
        var deletedUserWords = 0

        appDatabase.withTransaction {
                // Mevcut UID'yi delete öncesi cache'le — restore sonrası hesap eşleşmesini korur.
                val preservedUid = settingsDao.getSettings()?.firebaseUid.orEmpty()

                quizAnswerDao.deleteAll()
                quizSessionDao.deleteAll()
                wordProgressDao.deleteAll()
                wordSampleDao.deleteAll()
                storyDao.clearAllStories()
                settingsDao.deleteAll()
                deletedUserWords = wordDao.deleteAllUserWords()

                // User words insert + engWord → newWordId mapping.
                // BUG #7 fix: System ve user mappinglerini ayrı tutuyoruz; user word'ün engWord'ü
                // sistemle çakışırsa system kayıtları override edilmiyor — progress lookup'ta
                // önce user, bulamazsa system map'i denenir.
                val systemMap = mutableMapOf<String, Int>()
                val userMap = mutableMapOf<String, Int>()

                wordDao.getAllWords().forEach { systemMap[it.engWord] = it.wordId }

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
                    userMap[dto.engWord] = newId
                }

                // Samples — sample'lar genelde user word'lere bağlı; user öncelikli.
                val sampleEntities = data.wordSamples.mapNotNull {
                    val wid = userMap[it.engWord] ?: systemMap[it.engWord] ?: return@mapNotNull null
                    WordSampleEntity(wordId = wid, sentence = it.sentence)
                }
                if (sampleEntities.isNotEmpty()) wordSampleDao.insertSamples(sampleEntities)

                // Progress restore: backup'taki her progress dto'sunu hem user hem system map'i ile dener.
                // Aynı engWord her iki map'te de varsa İKİ ayrı progress kaydı oluşturulur — biri user
                // biri system için. Bu, ileride istatistik bozulmasına engel olur (BUG #7).
                val widToEng = mutableMapOf<Int, String>()
                systemMap.forEach { (eng, wid) -> widToEng[wid] = eng }
                userMap.forEach { (eng, wid) -> widToEng[wid] = eng }

                // Defansif dedup: eski v1 backuplarda aynı engWord için birden fazla DTO olabilir
                // (sistem + user word çakışması). En bilgili olanı (totalAttempts en yüksek) koru.
                val dedupedProgress = data.progress
                    .groupBy { it.engWord }
                    .mapNotNull { (_, candidates) -> pickRichestProgress(candidates) }
                val duplicatesSkipped = data.progress.size - dedupedProgress.size

                dedupedProgress.forEach { dto ->
                    val targets = listOfNotNull(systemMap[dto.engWord], userMap[dto.engWord]).distinct()
                    if (targets.isEmpty()) {
                        progressDropped++
                        return@forEach
                    }
                    targets.forEach { wid ->
                        wordProgressDao.insertProgress(
                            WordProgressEntity(
                                wordId = wid,
                                correctStreak = dto.correctStreak,
                                reviewStage = dto.reviewStage,
                                totalCorrect = dto.totalCorrect,
                                totalAttempts = dto.totalAttempts,
                                nextReviewDate = dto.nextReviewDate,
                                lastAnsweredDate = dto.lastAnsweredDate,
                                isLearned = if (dto.isLearned) 1 else 0,
                                lastShownDate = dto.lastShownDate
                            )
                        )
                        progressRestored++
                    }
                }
                progressDropped += duplicatesSkipped
                // Yeni atanan progressId'leri oku ve eng map'ini kur.
                // Aynı engWord birden fazla wid'e bağlıysa progressByEng son işlenen wid'in id'sini tutar;
                // quiz_answers restore'unda her iki kayıt da bağlanmasa bile en azından bir kayıt bağlanır.
                val progressByEng = mutableMapOf<String, Int>()
                wordProgressDao.getAllProgress().forEach { p ->
                    val eng = widToEng[p.wordId] ?: return@forEach
                    progressByEng[eng] = p.progressId
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
                    val sid = oldToNewSession[a.originalSessionId]
                    val pid = progressByEng[a.engWord]
                    if (sid == null || pid == null) {
                        answersDropped++
                        return@forEach
                    }
                    quizAnswerDao.insertAnswer(
                        QuizAnswerEntity(
                            sessionId = sid,
                            progressId = pid,
                            isCorrect = if (a.isCorrect) 1 else 0,
                            answeredAt = a.answeredAt
                        )
                    )
                    answersRestored++
                }

                // Settings — UID delete öncesi cache'lendi; geri yaz.
                data.settings?.let { dto ->
                    settingsDao.insertSettings(
                        SettingsEntity(
                            id = 1,
                            firebaseUid = preservedUid,
                            displayName = dto.displayName,
                            dailyNewWordCount = dto.dailyNewWordCount,
                            userLevel = dto.userLevel,
                            updatedAt = dto.updatedAt
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
            }   // withTransaction kapanışı

        return RestoreCounts(
            deletedUserWords = deletedUserWords,
            progressRestored = progressRestored,
            progressDropped = progressDropped,
            answersRestored = answersRestored,
            answersDropped = answersDropped
        )
    }

    // ---------- helpers ----------

    /**
     * Aynı engWord'e sahip birden fazla progress DTO arasında en zengin bilgi taşıyanı seçer.
     * Skor: totalAttempts ağırlıklı (öne çıkar) + totalCorrect + öğrenilmiş bonus.
     * Boş liste null döner (mapNotNull ile zincire takılır).
     */
    private fun pickRichestProgress(candidates: List<WordProgressDto>): WordProgressDto? =
        candidates.maxByOrNull {
            it.totalAttempts * 1_000_000L + it.totalCorrect + (if (it.isLearned) 1 else 0)
        }

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
