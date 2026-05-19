package com.app.wordlearn.domain.model

data class Story(
    val id: Long = 0,
    val words: List<String>,
    val storyText: String,
    val imagePath: String?,
    val isLocalImage: Boolean,
    val createdAt: Long
)