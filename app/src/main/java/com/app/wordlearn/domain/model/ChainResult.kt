package com.app.wordlearn.domain.model

data class ChainResult(
    val words: List<String>,
    val story: String,
    val imagePath: String? = null
)
