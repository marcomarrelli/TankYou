package project.unibo.tankyou.data.repositories

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import project.unibo.tankyou.data.DatabaseClient

/**
 * Repository class that handles user authentication operations using Supabase Auth.
 *
 * Implements singleton pattern and provides methods for user registration, login, logout, and password reset.
 */
class AuthRepository {
    private val auth: Auth = DatabaseClient.client.auth

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        /**
         * Returns the singleton instance of AuthRepository.
         *
         * @return the singleton AuthRepository instance
         */
        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Flow that emits the current session status changes.
     */
    val sessionStatus: Flow<SessionStatus> = auth.sessionStatus

    /**
     * The currently authenticated user information, null if no user is logged in.
     */
    val currentUser: UserInfo? = auth.currentUserOrNull()

    /**
     * Registers a new user with email and password.
     *
     * @param email the user's email address
     * @param password the user's password
     *
     * @return Result containing [Unit] on success or [Exception] on failure
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param email the user's email address
     * @param password the user's password
     *
     * @return Result containing [Unit] on success or [Exception] on failure
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated user.
     *
     * @return Result containing [Unit] on success or [Exception] on failure
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if a user is authenticated, false otherwise
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null
    }

    /**
     * Sends a password reset email to the specified email address.
     *
     * @param email the email address to send the reset link to
     *
     * @return Result containing [Unit] on success or [Exception] on failure
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}