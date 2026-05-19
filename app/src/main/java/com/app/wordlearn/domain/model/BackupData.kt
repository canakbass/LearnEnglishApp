package com.app.wordlearn.domain.model

import kotlinx.serialization.Serializable

/**
 * Yedek dosyasının (data.json) içeriği.
 *
 * Çevrim dışı, taşınabilir bir snapshot:
 *  - Kullanıcının eklediği kelimeler (source = "user") ve örnek cümleleri
 *  - Hem sistem hem kullanıcı kelimeleri için ilerleme kayıtları (eşleştirme [engWord] üzerinden yapılır)
 *  - Quiz oturumları + cevapları
 *  - Yerel ayarlar
 *  - Kayıtlı Word Chain hikayeleri
 *
 * Görsel dosyaları ayrı `images/` klasöründe ZIP içinde taşınır;
 * burada sadece dosya adı (path değil) tutulur.
 */
@Serializable
data class BackupData(
    /** Format sürümü; gelecekteki migration'lar için. */
    val schemaVersion: Int = SCHEMA_VERSION,
    /** Uygulamanın yedek alındığı andaki sürümü. */
    val appVersionName: String = "",
    /** Yedek dosyası oluşturulma zamanı (epoch ms). */
    val createdAt: Long = 0L,
    val userWords: List<WordDto> = emptyList(),
    val wordSamples: List<WordSampleDto> = emptyList(),
    val progress: List<WordProgressDto> = emptyList(),
    val quizSessions: List<QuizSessionDto> = emptyList(),
    val quizAnswers: List<QuizAnswerDto> = emptyList(),
    val settings: SettingsDto? = null,
    val stories: List<StoryDto> = emptyList()
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class WordDto(
    /** Orijinal yerel ID — restore sırasında remap için ip ucu olarak kullanılır. */
    val originalWordId: Int,
    val engWord: String,
    val turWord: String,
    /** ZIP içindeki `images/` klasöründeki dosya adı, yoksa null. */
    val pictureFileName: String? = null,
    val audioFileName: String? = null,
    val level: String,
    val category: String,
    val createdAt: Long
)

@Serializable
data class WordSampleDto(
    /** Ait olduğu kelimenin İngilizcesi — engWord ile remap edilir. */
    val engWord: String,
    val sentence: String
)

@Serializable
data class WordProgressDto(
    /** İlerleme kaydının ait olduğu kelimenin İngilizcesi. */
    val engWord: String,
    val correctStreak: Int,
    val reviewStage: Int,
    val totalCorrect: Int,
    val totalAttempts: Int,
    val nextReviewDate: Long,
    val lastAnsweredDate: Long,
    val isLearned: Boolean
)

@Serializable
data class QuizSessionDto(
    val originalSessionId: Int,
    val sessionDate: Long,
    val totalQuestions: Int,
    val correctCount: Int,
    val durationSeconds: Int
)

@Serializable
data class QuizAnswerDto(
    val originalSessionId: Int,
    /** Hangi kelime için cevap verildi — engWord ile remap edilir. */
    val engWord: String,
    val isCorrect: Boolean,
    val answeredAt: Long
)

@Serializable
data class SettingsDto(
    val displayName: String,
    val dailyNewWordCount: Int,
    val userLevel: String,
    val updatedAt: Long
)

@Serializable
data class StoryDto(
    val words: String,
    val storyText: String,
    val imageFileName: String? = null,
    val isLocalImage: Boolean = false,
    val createdAt: Long
)
