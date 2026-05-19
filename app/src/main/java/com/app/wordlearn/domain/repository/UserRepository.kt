package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.User

interface UserRepository {
    suspend fun registerUser(username: String, passwordHash: String, email: String? = null): Result<User>
    suspend fun loginUser(username: String, passwordHash: String): Result<User>
    suspend fun loginWithGoogleToken(idToken: String): Result<User>
    suspend fun loginAsGuest(): Result<User>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun isEmailVerified(): Boolean
    suspend fun getUser(userId: String): User?
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getCurrentUserName(): String?
    fun getCurrentUserId(): String?
    fun isGuest(): Boolean
    suspend fun updateDisplayName(newName: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
