package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_samples",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["wordId"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WordSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val sampleId: Int = 0,
    val wordId: Int,
    val sentence: String
)
