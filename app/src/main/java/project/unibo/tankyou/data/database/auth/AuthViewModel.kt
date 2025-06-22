package project.unibo.tankyou.data.database.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import project.unibo.tankyou.utils.Constants

/**
 * ViewModel responsible for managing authentication state and operations.
 *
 * This class handles user authentication, including
 * sign-in, sign-up, sign-out, and guest mode functionality.
 * It maintains the current authentication state
 * and provides reactive streams for UI components to observe.
 *
 * @author TankYou Team
 * @since 1.0
 */
class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val _isGuestMode = MutableStateFlow(false)

    /**
     * StateFlow that emits the current authentication state.
     * Observers can collect this flow to react to authentication changes.
     */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * StateFlow that indicates whether the user is in guest mode.
     * Guest mode allows limited app functionality without authentication.
     */
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Checks the current authentication status and updates the state accordingly.
     * This method is called during initialization to restore the previous auth state.
     */
    private fun checkAuthStatus() {
        Log.d(Constants.App.LOG_TAG, "Checking authentication status...")

        if (Constants.App.AUTH_REPOSITORY.isUserLoggedIn()) {
            Log.i(Constants.App.LOG_TAG, "User is already logged in!")
            _authState.value = AuthState.Authenticated
            _isGuestMode.value = false
        } else {
            Log.d(Constants.App.LOG_TAG, "User is not logged in")
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Attempts to sign in a user with email and password.
     *
     * This method performs asynchronous authentication and updates the auth state
     * based on the result. During the process, the state is set to Loading.
     *
     * @param email The user's email address for authentication
     * @param password The user's password for authentication
     */
    fun signIn(email: String, password: String) {
        Log.i(Constants.App.LOG_TAG, "Attempting to sign in user with email: $email")

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val result = Constants.App.AUTH_REPOSITORY.signIn(email, password)
                _authState.value = if (result.isSuccess) {
                    Log.i(Constants.App.LOG_TAG, "Sign in successful for user: $email")
                    _isGuestMode.value = false
                    AuthState.Authenticated
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Login error"
                    Log.e(
                        Constants.App.LOG_TAG,
                        "Sign in failed for user: $email. Error: $errorMessage"
                    )
                    AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Unexpected error during sign in for user: $email", e)
                _authState.value = AuthState.Error("Unexpected login error occurred")
            }
        }
    }

    /**
     * Attempts to create a new user account with the provided information.
     *
     * This method performs asynchronous user registration and updates the auth state
     * based on the result. During the process, the state is set to Loading.
     *
     * @param email The user's email address for the new account
     * @param password The password for the new account
     * @param name The user's first name
     * @param surname The user's last name
     * @param username The desired username for the account
     */
    fun signUp(email: String, password: String, name: String, surname: String, username: String) {
        Log.i(
            Constants.App.LOG_TAG,
            "Attempting to sign up new user with email: $email, username: $username"
        )

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val result =
                    Constants.App.AUTH_REPOSITORY.signUp(email, password, name, surname, username)
                _authState.value = if (result.isSuccess) {
                    Log.i(Constants.App.LOG_TAG, "Sign up successful for user: $email")
                    _isGuestMode.value = false
                    AuthState.Authenticated
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Registration error"
                    Log.e(
                        Constants.App.LOG_TAG,
                        "Sign up failed for user: $email. Error: $errorMessage"
                    )
                    AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Unexpected error during sign up for user: $email", e)
                _authState.value = AuthState.Error("Unexpected registration error occurred")
            }
        }
    }

    /**
     * Signs out the current authenticated user.
     *
     * This method performs asynchronous sign-out and resets the authentication state
     * to Unauthenticated if successful. Guest mode is also disabled.
     */
    fun signOut() {
        Log.i(Constants.App.LOG_TAG, "Attempting to sign out current user")

        viewModelScope.launch {
            try {
                val result = Constants.App.AUTH_REPOSITORY.signOut()

                if (result.isSuccess) {
                    Log.i(Constants.App.LOG_TAG, "Sign out successful")
                    _authState.value = AuthState.Unauthenticated
                    _isGuestMode.value = false
                } else {
                    Log.w(
                        Constants.App.LOG_TAG,
                        "Sign out completed with warnings: ${result.exceptionOrNull()?.message}"
                    )

                    // Still setting to unauthenticated even if there were warnings
                    _authState.value = AuthState.Unauthenticated
                    _isGuestMode.value = false
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Unexpected error during sign out", e)

                // Force sign out locally even if remote sign out failed
                _authState.value = AuthState.Unauthenticated
                _isGuestMode.value = false
            }
        }
    }

    /**
     * Enables guest mode for the application.
     *
     * Guest mode allows users to use limited app functionality without creating
     * an account or signing in. The authentication state remains Unauthenticated
     * but guest mode is enabled.
     */
    fun enterAsGuest() {
        Log.i(Constants.App.LOG_TAG, "User entering as a guest")

        _isGuestMode.value = true
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Checks if the current user's email address has been verified.
     *
     * @return if the user is authenticated and their email is verified
     */
    fun isCurrentUserEmailVerified(): Boolean {
        val isVerified = Constants.App.AUTH_REPOSITORY.isEmailVerified()

        Log.d(Constants.App.LOG_TAG, "Email verification status: $isVerified")

        return isVerified
    }
}