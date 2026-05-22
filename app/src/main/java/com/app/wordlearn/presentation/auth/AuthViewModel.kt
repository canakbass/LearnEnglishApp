package com.app.wordlearn.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.repository.SettingsRepository
import com.app.wordlearn.domain.repository.UserRepository
import com.app.wordlearn.domain.usecase.InitializeDatabaseUseCase
import com.app.wordlearn.domain.util.CrashReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val isGuest: Boolean = false,
    /** Hata mesajı (kırmızı snackbar olarak gösterilir). */
    val errorMessage: String? = null,
    /** Bilgilendirme/başarı mesajı (yeşil/info snackbar). */
    val infoMessage: String? = null,
    val isLoading: Boolean = true,
    val needsEmailVerification: Boolean = false,
    val verificationEmail: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val initializeDatabaseUseCase: InitializeDatabaseUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isLoading = true))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        initializeApp()
    }

    private fun initializeApp() {
        // Veritabanı seed'ini auth check'i bloklamadan arka planda başlat.
        // Emülatörde slow disk I/O nedeniyle siyah ekran riski azalır.
        viewModelScope.launch {
            try {
                initializeDatabaseUseCase.execute()
                Log.d(TAG, "Database seeding finished")
            } catch (e: Exception) {
                // Seeding hatası uygulamayı kilitlemesin; non-fatal olarak raporla.
                CrashReporter.reportException(TAG, "Database seeding failed", e)
            }
        }

        // Auth check ayrı bir coroutine'de — paralel çalışır.
        viewModelScope.launch {
            Log.d(TAG, "initializeApp: checking auth state")
            val loggedIn = withTimeoutOrNull(AUTH_CHECK_TIMEOUT_MS) {
                runCatching { userRepository.isLoggedIn() }.getOrDefault(false)
            } ?: false

            if (loggedIn) {
                val userName = runCatching { userRepository.getCurrentUserName() }
                    .getOrNull()
                    .orEmpty()
                val uid = runCatching { userRepository.getCurrentUserId() }.getOrNull()
                val guest = runCatching { userRepository.isGuest() }.getOrDefault(false)
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userName = if (userName.isBlank() && guest) "Misafir" else userName,
                    isGuest = guest,
                    isLoading = false
                )
                CrashReporter.setUserId(uid)
                CrashReporter.log(TAG, "Restored existing session for uid=$uid")
                Log.d(TAG, "initializeApp: user logged in -> $userName")
            } else {
                _authState.value = _authState.value.copy(isLoading = false)
                Log.d(TAG, "initializeApp: no user -> login screen")
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
        private const val AUTH_CHECK_TIMEOUT_MS = 3_000L
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = _authState.value.copy(
                errorMessage = "Kullanıcı adı/e-posta ve şifre boş olamaz"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = userRepository.loginUser(username, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                val settings = settingsRepository.getSettings()
                settingsRepository.saveSettings(
                    settings.copy(displayName = user?.username ?: username)
                )
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userName = user?.username ?: username,
                    isLoading = false
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Giriş başarısız"
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        when {
            username.isBlank() || password.isBlank() -> {
                _authState.value = _authState.value.copy(
                    errorMessage = "Tüm alanları doldurunuz"
                )
            }
            email.isNotBlank() && !email.contains("@") -> {
                _authState.value = _authState.value.copy(
                    errorMessage = "Geçerli bir e-posta adresi giriniz"
                )
            }
            password != confirmPassword -> {
                _authState.value = _authState.value.copy(
                    errorMessage = "Şifreler uyuşmuyor"
                )
            }
            password.length < 6 -> {
                _authState.value = _authState.value.copy(
                    errorMessage = "Şifre en az 6 karakter olmalıdır"
                )
            }
            else -> {
                viewModelScope.launch {
                    _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
                    val actualEmail = email.ifBlank { null }
                    val result = userRepository.registerUser(username, password, actualEmail)
                    if (result.isSuccess) {
                        if (actualEmail != null) {
                            // E-posta girildiyse doğrulama gerekli
                            _authState.value = AuthState(
                                isLoading = false,
                                needsEmailVerification = true,
                                verificationEmail = actualEmail
                            )
                        } else {
                            // E-posta girilmediyse direkt giriş
                            val user = result.getOrNull()
                            val settings = settingsRepository.getSettings()
                            settingsRepository.saveSettings(
                                settings.copy(displayName = user?.username ?: username)
                            )
                            _authState.value = AuthState(
                                isLoggedIn = true,
                                userName = user?.username ?: username,
                                isLoading = false
                            )
                        }
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Kayıt başarısız"
                        )
                    }
                }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            userRepository.sendEmailVerification()
            _authState.value = _authState.value.copy(
                errorMessage = null
            )
        }
    }

    fun checkVerificationAndLogin() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            val verified = userRepository.isEmailVerified()
            if (verified) {
                val userName = userRepository.getCurrentUserName() ?: ""
                val settings = settingsRepository.getSettings()
                settingsRepository.saveSettings(settings.copy(displayName = userName))
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userName = userName,
                    isLoading = false
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = "E-posta henüz doğrulanmamış. Gelen kutunuzu kontrol edin."
                )
            }
        }
    }

    fun handleGoogleSignInResult(idToken: String?) {
        if (idToken == null) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Google ile giriş iptal edildi"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = userRepository.loginWithGoogleToken(idToken)
            if (result.isSuccess) {
                val user = result.getOrNull()
                val settings = settingsRepository.getSettings()
                settingsRepository.saveSettings(
                    settings.copy(displayName = user?.username ?: "Kullanıcı")
                )
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userName = user?.username ?: "Kullanıcı",
                    isLoading = false
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Giriş başarısız"
                )
            }
        }
    }

    fun handleGoogleSignInError(statusCode: Int) {
        val message = when (statusCode) {
            10 -> "Google yapılandırma hatası. Firebase konsolunda SHA-1 parmak izi ve Google oturum açmayı etkinleştirdiğinizden emin olun."
            7 -> "Ağ hatası. İnternet bağlantınızı kontrol edin."
            else -> "Google ile giriş başarısız (hata kodu: $statusCode)"
        }
        _authState.value = _authState.value.copy(
            isLoading = false,
            errorMessage = message
        )
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = _authState.value.copy(
                errorMessage = "Lütfen e-posta adresinizi giriniz"
            )
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            val result = userRepository.resetPassword(email)
            if (result.isSuccess) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    infoMessage = "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi."
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Şifre sıfırlama başarısız oldu"
                )
            }
        }
    }

    fun backToLogin() {
        viewModelScope.launch {
            userRepository.logout()
            _authState.value = AuthState(isLoading = false)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            CrashReporter.setUserId(null)
            _authState.value = AuthState(isLoading = false)
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = userRepository.loginAsGuest()
            if (result.isSuccess) {
                val user = result.getOrNull()
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userName = user?.username ?: "Misafir",
                    isGuest = true,
                    isLoading = false
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Misafir girişi başarısız"
                )
            }
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) {
            _authState.value = _authState.value.copy(errorMessage = "Kullanıcı adı boş olamaz")
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = userRepository.updateDisplayName(newName)
            if (result.isSuccess) {
                val settings = settingsRepository.getSettings()
                settingsRepository.saveSettings(settings.copy(displayName = newName))
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    userName = newName,
                    infoMessage = "Kullanıcı adı güncellendi."
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Güncelleme başarısız"
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = userRepository.deleteAccount()
            if (result.isSuccess) {
                CrashReporter.setUserId(null)
                _authState.value = AuthState(isLoading = false)
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Hesap silinemedi"
                )
            }
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }

    fun clearInfo() {
        _authState.value = _authState.value.copy(infoMessage = null)
    }
}