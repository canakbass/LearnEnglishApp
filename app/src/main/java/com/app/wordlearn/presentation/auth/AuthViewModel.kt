package com.app.wordlearn.presentation.auth

import androidx.lifecycle.ViewModel
import com.app.wordlearn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = _authState.value.copy(
                errorMessage = "Kullanıcı adı ve şifre boş olamaz"
            )
            return
        }

        _authState.value = AuthState(
            isLoggedIn = true,
            userName = username,
            isLoading = false
        )
    }

    fun register(username: String, password: String, confirmPassword: String) {
        when {
            username.isBlank() || password.isBlank() -> {
                _authState.value = _authState.value.copy(
                    errorMessage = "Tüm alanları doldurunuz"
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
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userName = username,
                    isLoading = false
                )
            }
        }
    }

    fun logout() {
        _authState.value = AuthState()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}
