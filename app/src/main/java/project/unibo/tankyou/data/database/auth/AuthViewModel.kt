package project.unibo.tankyou.data.database.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.repositories.AuthRepository
import project.unibo.tankyou.data.repositories.UserRepository

/**
 * [ViewModel] that manages user authentication state.
 */
class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository.getInstance()
    private val userRepository = UserRepository.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Checks the current user authentication status.
     */
    private fun checkAuthStatus() {
        if (authRepository.isUserLoggedIn()) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Signs in the user with provided credentials.
     *
     * @param email the user's email
     * @param password the user's password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Login error")
            }
        }
    }

    /**
     * Registers a new user with provided credentials.
     *
     * @param email the new user's email
     * @param password the new user's password
     */
    fun signUp(email: String, password: String, name: String, surname: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signUp(email, password, name, surname, username)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Registration error")
            }
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        viewModelScope.launch {
            val result = authRepository.signOut()
            if (result.isSuccess) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
}