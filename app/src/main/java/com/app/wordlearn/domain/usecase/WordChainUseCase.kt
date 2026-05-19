package com.app.wordlearn.domain.usecase

import com.app.wordlearn.BuildConfig
import com.app.wordlearn.domain.model.ChainResult
import com.app.wordlearn.domain.repository.WordRepository
import com.app.wordlearn.domain.repository.StoryRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class WordChainUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val storyRepository: StoryRepository
) {
    private val generativeModel: GenerativeModel? by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            GenerativeModel(
                modelName = "gemini-flash-latest",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.9f
                    maxOutputTokens = 2000
                }
            )
        } else {
            null
        }
    }

    suspend fun execute(inputWords: List<String>? = null): ChainResult {
        // 1. Kelime listesi al
        val words = if (inputWords != null && inputWords.size >= 5) {
            inputWords.take(5)
        } else {
            selectChainWords()
        }

        // 2. Zincir kuralı: son harf = sonraki ilk harf
        val chainWords = buildChainFromPool(words)

        // 3. Gemini ile hikaye oluştur
        val story = generateStoryWithAI(chainWords)

        // 4. Pollinations AI ile hikayeyi resmetmek için bir görsel URL'si oluştur
        val imagePrompt = "A beautiful illustration representing the following words: ${chainWords.joinToString(", ")}. Fantasy art style, high quality, vibrant colors."
        val encodedPrompt = URLEncoder.encode(imagePrompt, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        val seed = (0..1000000).random()
        val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=800&height=600&nologo=true&seed=$seed"
        // 5. Hikayeyi ve resmi DB'ye kaydet
        // Resim cihazda arkaplanda kopyalanacak (StoryRepositoryImpl icerisinde hallediliyor)
        storyRepository.saveStory(
            words = chainWords,
            storyText = story,
            imageUrl = imageUrl
        )
        return ChainResult(
            words = chainWords,
            story = story,
            imagePath = imageUrl
        )
    }

    private suspend fun selectChainWords(): List<String> {
        val allWords = wordRepository.getAllWords()
            .map { it.engWord }
            .filter { it.isNotEmpty() }
            .distinct()
            .shuffled()

        if (allWords.size < 5) return allWords

        return buildChainFromPool(allWords)
    }

    private fun buildChainFromPool(pool: List<String>): List<String> {
        val result = mutableListOf<String>()
        val remaining = pool.toMutableList()

        // İlk kelimeyi seç
        val first = remaining.removeAt(0)
        result.add(first)

        // Zincir kur: son harf = sonraki ilk harf
        repeat(4) {
            val lastChar = result.last().last().lowercaseChar()
            val next = remaining.find { it.first().lowercaseChar() == lastChar }

            if (next != null) {
                result.add(next)
                remaining.remove(next)
            } else if (remaining.isNotEmpty()) {
                val random = remaining.removeAt(0)
                result.add(random)
            }
        }

        return result.take(5)
    }

    private suspend fun generateStoryWithAI(words: List<String>): String {
        if (words.isEmpty()) return ""

        val model = generativeModel
        if (model == null) {
            return generateFallbackStory(words)
        }

        return try {
            val prompt = buildString {
                append("Create a short, fun, and educational story (4-5 sentences) ")
                append("using ALL of these English words: ${words.joinToString(", ")}. ")
                append("The story should be simple enough for A1-B1 level English learners. ")
                append("Bold the target words in the story using **word** format. ")
                append("After the story, add a Turkish translation of the story. ")
                append("CRITICAL: Do NOT add greetings, introductions, or conversational text like 'Hello, I am your teacher' or 'Here is your story'. Output ONLY the story and the translation directly. ")
                append("Format:\n")
                append("📖 Story:\n[English story here]\n\n")
                append("🇹🇷 Türkçe:\n[Turkish translation here]")
            }

            val response = model.generateContent(prompt)
            response.text ?: generateFallbackStory(words)
        } catch (e: Exception) {
            android.util.Log.e("WordChainUseCase", "Gemini API Error: ${e.message}", e)
            generateFallbackStory(words) + "\n\n⚠️ Hata: ${e.localizedMessage}"
        }
    }

    private fun generateFallbackStory(words: List<String>): String {
        val highlighted = words.joinToString(", ") { "**$it**" }
        return buildString {
            append("📖 Story:\n")
            append("One day, a student was learning the word **${words.getOrElse(0) { "word" }}**. ")
            if (words.size > 1) append("While studying, they found **${words[1]}** in their book. ")
            if (words.size > 2) append("The teacher showed them a **${words[2]}** example. ")
            if (words.size > 3) append("With **${words[3]}**, everything became clearer. ")
            if (words.size > 4) append("Finally, they felt **${words[4]}** about learning English!")
            append("\n\nKullanılan kelimeler: $highlighted")
        }
    }
}
