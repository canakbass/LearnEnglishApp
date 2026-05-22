package com.app.wordlearn.domain.model

data class QuizQuestion(
    val wordId: Int,
    val questionText: String,
    val correctAnswer: String,
    val options: List<String>,
    val picturePath: String? = null,
    /** Kelimeye ait kullanıcı örnek cümleleri; varsa quiz ekranında küçük fontla gösterilir. */
    val sampleSentences: List<String> = emptyList()
)
