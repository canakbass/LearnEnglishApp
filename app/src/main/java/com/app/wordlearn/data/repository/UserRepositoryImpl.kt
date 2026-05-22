package com.app.wordlearn.data.repository

import com.app.wordlearn.data.local.AppDatabase
import com.app.wordlearn.domain.model.User
import com.app.wordlearn.domain.repository.UserRepository
import com.app.wordlearn.domain.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val auth: FirebaseAuth
) : UserRepository {

    override suspend fun registerUser(username: String, passwordHash: String, email: String?): Result<User> {
        if (email.isNullOrBlank()) {
            return Result.failure(Exception("E-posta adresi zorunludur."))
        }
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, passwordHash).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Set display name
                val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                firebaseUser.updateProfile(profileUpdate).await()

                // Send verification email
                firebaseUser.sendEmailVerification().await()

                val user = User(
                    userId = firebaseUser.uid,
                    username = username,
                    email = email,
                    level = Constants.USER_LEVEL_BEGINNER,
                    score = 0,
                    joinDate = System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Kayıt başarısız."))
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("email address is already in use") == true -> "Bu e-posta adresi zaten kullanılıyor."
                e.message?.contains("badly formatted") == true -> "Geçersiz e-posta formatı."
                e.message?.contains("weak password") == true -> "Şifre en az 6 karakter olmalıdır."
                else -> e.message ?: "Kayıt sırasında hata oluştu."
            }
            Result.failure(Exception(message))
        }
    }

    override suspend fun loginUser(email: String, passwordHash: String): Result<User> {
        return try {
            if (!email.contains("@")) {
                return Result.failure(Exception("Lütfen giriş yapmak için e-posta adresinizi kullanın."))
            }
            val authResult = auth.signInWithEmailAndPassword(email, passwordHash).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Check email verification
                if (!firebaseUser.isEmailVerified) {
                    auth.signOut()
                    return Result.failure(Exception("E-posta adresiniz doğrulanmamış. Lütfen gelen kutunuzu kontrol edin."))
                }

                val user = User(
                    userId = firebaseUser.uid,
                    username = firebaseUser.displayName ?: email.substringBefore("@"),
                    email = email,
                    level = Constants.USER_LEVEL_BEGINNER,
                    score = 0,
                    joinDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Giriş başarısız."))
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("no user record") == true -> "Kullanıcı bulunamadı."
                e.message?.contains("password is invalid") == true -> "Hatalı şifre."
                e.message?.contains("blocked") == true -> "Çok fazla başarısız deneme. Lütfen daha sonra tekrar deneyin."
                else -> e.message ?: "Giriş sırasında hata oluştu."
            }
            Result.failure(Exception(message))
        }
    }

    override suspend fun loginWithGoogleToken(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val user = User(
                    userId = firebaseUser.uid,
                    username = firebaseUser.displayName ?: "Kullanıcı",
                    email = firebaseUser.email,
                    level = Constants.USER_LEVEL_BEGINNER,
                    score = 0,
                    joinDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Google ile giriş başarısız."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Google giriş hatası."))
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("user record") == true -> "Bu e-posta adresiyle kayıtlı bir hesap bulunamadı."
                e.message?.contains("badly formatted") == true -> "Geçersiz e-posta formatı."
                else -> e.message ?: "Şifre sıfırlama e-postası gönderilemedi."
            }
            Result.failure(Exception(msg))
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified ?: false
    }

    override suspend fun getUser(userId: String): User? {
        val firebaseUser = auth.currentUser ?: return null
        if (firebaseUser.uid != userId) return null
        return User(
            userId = firebaseUser.uid,
            username = firebaseUser.displayName ?: "",
            email = firebaseUser.email,
            level = Constants.USER_LEVEL_BEGINNER,
            score = 0,
            joinDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }

    override suspend fun loginAsGuest(): Result<User> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = User(
                    userId = firebaseUser.uid,
                    username = "Misafir",
                    email = null,
                    level = Constants.USER_LEVEL_BEGINNER,
                    score = 0,
                    joinDate = System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Misafir girişi başarısız."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Misafir girişi sırasında hata oluştu."))
        }
    }

    override fun isGuest(): Boolean {
        return auth.currentUser?.isAnonymous == true
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı oturumu bulunamadı."))
            val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdate).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Kullanıcı adı güncellenemedi."))
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı oturumu bulunamadı."))
            appDatabase.clearUserData()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Hesap silinemedi."))
        }
    }

    override suspend fun logout() {
        auth.signOut()
        appDatabase.clearUserData()
    }

    override fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
