package project.unibo.tankyou.data.repositories

import android.util.Log
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.utils.Constants

/**
 * Repository class responsible for managing authentication operations.
 * Handles user registration, login, logout, and email verification using Supabase Auth.
 */
class AuthRepository {
    private val auth: Auth = DatabaseClient.client.auth

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        /**
         * Returns the singleton instance of AuthRepository.
         * Thread-safe implementation using double-checked locking pattern.
         *
         * @return The singleton instance of AuthRepository
         */
        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Gets the currently authenticated user information.
     *
     * @return UserInfo if a user is logged in, null otherwise
     */
    val currentUser: UserInfo?
        get() = auth.currentUserOrNull()

    /**
     * Registers a new user with email, password, and additional user data.
     *
     * @param email The user's email address
     * @param password The user's password
     * @param name The user's first name
     * @param surname The user's surname
     * @param username The user's chosen username
     * @return Result containing Unit on success or Exception on failure
     */
    @OptIn(SupabaseInternal::class)
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        surname: String,
        username: String
    ): Result<Unit> {
        return try {
            Log.d(
                Constants.App.LOG_TAG,
                "Attempting to sign up user with email: $email, username: $username"
            )

            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("name", JsonPrimitive(name))
                    put("surname", JsonPrimitive(surname))
                    put("username", JsonPrimitive(username))
                }
            }

            Log.d(Constants.App.LOG_TAG, "User sign up successful for email: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to sign up user with email: $email", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with email and password.
     *
     * @param email The user's email address
     * @param password The user's password
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            Log.d(Constants.App.LOG_TAG, "Attempting to sign in user with email: $email")

            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            Log.d(Constants.App.LOG_TAG, "User sign in successful for email: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to sign in user with email: $email", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated user.
     *
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            val userEmail = currentUser?.email
            Log.d(Constants.App.LOG_TAG, "Attempting to sign out user: $userEmail")

            auth.signOut()

            Log.d(Constants.App.LOG_TAG, "User sign out successful for: $userEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to sign out user", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if a user is logged in, false otherwise
     */
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = auth.currentUserOrNull() != null
        Log.d(Constants.App.LOG_TAG, "User logged in status: $isLoggedIn")
        return isLoggedIn
    }

    /**
     * Checks if the current user's email is verified.
     *
     * @return true if the user's email is verified, false otherwise
     */
    fun isEmailVerified(): Boolean {
        val user = auth.currentUserOrNull()
        val isVerified = user?.emailConfirmedAt != null
        Log.d(
            Constants.App.LOG_TAG,
            "Email verification status for user ${user?.email}: $isVerified"
        )
        return isVerified
    }
}