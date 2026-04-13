package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.ChainResult
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

class WordChainUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    suspend fun execute(inputWords: List<String>? = null): ChainResult {
        // 1. Kelime listesi al (kullanıcıdan veya rastgele)
        val words = if (inputWords != null && inputWords.size >= 5) {
            inputWords.take(5)
        } else {
            selectChainWords()
        }

        // 2. Zincir kuralı: son harf = sonraki ilk harf
        val chainWords = buildChain(words)

        // 3. Hikaye oluştur (Gemini API yerine basit şablon)
        val story = generateStory(chainWords)

        return ChainResult(
            words = chainWords,
            story = story,
            imagePath = null
        )
    }

    private suspend fun selectChainWords(): List<String> {
        val allWords = wordRepository.getAllWords()
            .map { it.engWord }
            .filter { it.isNotEmpty() }
            .shuffled()

        if (allWords.size < 5) return allWords

        // Zincir oluşturmayı dene
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
                // Uygun kelime bulunamazsa rastgele ekle
                val random = remaining.removeAt(0)
                result.add(random)
            }
        }

        return result.take(5)
    }

    private fun buildChain(words: List<String>): List<String> {
        if (words.size <= 1) return words
        // Zincir düzenini koru
        return words
    }

    private fun generateStory(words: List<String>): String {
        if (words.isEmpty()) return ""

        // Gemini API entegrasyonu yapılacak
        // Şimdilik basit bir şablon hikaye
        val highlighted = words.joinToString(", ") { "**$it**" }
        return buildString {
            append("Bir gün ${words[0]} ile başlayan muhteşem bir macera yaşandı. ")
            if (words.size > 1) append("${words[1]} boyunca ilerlerken ")
            if (words.size > 2) append("bir ${words[2]} ile karşılaştı. ")
            if (words.size > 3) append("${words[3]} sayesinde güvenli yolu buldu ")
            if (words.size > 4) append("ve ${words[4]} bir kahraman olarak anıldı.")
            append("\n\nKullanılan kelimeler: $highlighted")
        }
    }
}
