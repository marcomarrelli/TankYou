package project.unibo.tankyou.data.database.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.repositories.AuthRepository
import project.unibo.tankyou.data.repositories.UserRepository

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository.getInstance()
    private val userRepository = UserRepository.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        if (authRepository.isUserLoggedIn()) {
            _authState.value = AuthState.Authenticated
            _isGuestMode.value = false
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                _isGuestMode.value = false
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Login error")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, surname: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signUp(email, password, name, surname, username)
            _authState.value = if (result.isSuccess) {
                _isGuestMode.value = false
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Registration error")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val result = authRepository.signOut()
            if (result.isSuccess) {
                _authState.value = AuthState.Unauthenticated
                _isGuestMode.value = false
            }
        }
    }

    fun continueAsGuest() {
        _isGuestMode.value = true
        _authState.value = AuthState.Unauthenticated
    }

    fun isEmailVerified(): Boolean {
        return authRepository.isEmailVerified()
    }

    fun resendEmailVerification() {
        viewModelScope.launch {
            authRepository.resendEmailVerification()
        }
    }
}