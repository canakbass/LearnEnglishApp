package com.app.wordlearn.data.local

import com.app.wordlearn.data.local.entity.*
import com.app.wordlearn.domain.model.*

// Word mappers
fun WordEntity.toDomain() = Word(
    wordId = wordId,
    engWord = engWord,
    turWord = turWord,
    picturePath = picturePath,
    audioPath = audioPath,
    level = level,
    category = category,
    source = source,
    createdAt = createdAt
)

fun Word.toEntity() = WordEntity(
    wordId = wordId,
    engWord = engWord,
    turWord = turWord,
    picturePath = picturePath,
    audioPath = audioPath,
    level = level,
    category = category,
    source = source,
    createdAt = createdAt
)

// WordSample mappers
fun WordSampleEntity.toDomain() = WordSample(
    sampleId = sampleId,
    wordId = wordId,
    sentence = sentence
)

fun WordSample.toEntity() = WordSampleEntity(
    sampleId = sampleId,
    wordId = wordId,
    sentence = sentence
)

// WordProgress mappers
fun WordProgressEntity.toDomain() = WordProgress(
    progressId = progressId,
    wordId = wordId,
    correctStreak = correctStreak,
    reviewStage = reviewStage,
    totalCorrect = totalCorrect,
    totalAttempts = totalAttempts,
    nextReviewDate = nextReviewDate,
    lastAnsweredDate = lastAnsweredDate,
    isLearned = isLearned == 1
)

fun WordProgress.toEntity() = WordProgressEntity(
    progressId = progressId,
    wordId = wordId,
    correctStreak = correctStreak,
    reviewStage = reviewStage,
    totalCorrect = totalCorrect,
    totalAttempts = totalAttempts,
    nextReviewDate = nextReviewDate,
    lastAnsweredDate = lastAnsweredDate,
    isLearned = if (isLearned) 1 else 0
)

// QuizSession mappers
fun QuizSessionEntity.toDomain() = QuizSession(
    sessionId = sessionId,
    sessionDate = sessionDate,
    totalQuestions = totalQuestions,
    correctCount = correctCount,
    durationSeconds = durationSeconds
)

fun QuizSession.toEntity() = QuizSessionEntity(
    sessionId = sessionId,
    sessionDate = sessionDate,
    totalQuestions = totalQuestions,
    correctCount = correctCount,
    durationSeconds = durationSeconds
)

// QuizAnswer mappers
fun QuizAnswerEntity.toDomain() = QuizAnswer(
    answerId = answerId,
    sessionId = sessionId,
    progressId = progressId,
    isCorrect = isCorrect == 1,
    answeredAt = answeredAt
)

fun QuizAnswer.toEntity() = QuizAnswerEntity(
    answerId = answerId,
    sessionId = sessionId,
    progressId = progressId,
    isCorrect = if (isCorrect) 1 else 0,
    answeredAt = answeredAt
)

// Settings mappers
fun SettingsEntity.toDomain() = Settings(
    id = id,
    firebaseUid = firebaseUid,
    displayName = displayName,
    dailyNewWordCount = dailyNewWordCount,
    userLevel = userLevel,
    updatedAt = updatedAt
)

fun Settings.toEntity() = SettingsEntity(
    id = id,
    firebaseUid = firebaseUid,
    displayName = displayName,
    dailyNewWordCount = dailyNewWordCount,
    userLevel = userLevel,
    updatedAt = updatedAt
)
