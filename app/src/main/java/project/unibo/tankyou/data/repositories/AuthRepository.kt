package project.unibo.tankyou.data.repositories

import android.util.Log
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.Flow
import project.unibo.tankyou.data.DatabaseClient

class AuthRepository {
    private val auth: Auth = DatabaseClient.client.auth

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
        }
    }

    val sessionStatus: Flow<SessionStatus> = auth.sessionStatus

    val currentUser: UserInfo? = auth.currentUserOrNull()

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d("AuthRepository", "Utente registrato con successo: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Errore durante la registrazione", e)
            Result.failure(e)
        }
    }

    // Login con email e password
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d("AuthRepository", "Login effettuato con successo: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Errore durante il login", e)
            Result.failure(e)
        }
    }

    // Logout
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Log.d("AuthRepository", "Logout effettuato con successo")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Errore durante il logout", e)
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Log.d("AuthRepository", "Email di reset password inviata a: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Errore durante il reset password", e)
            Result.failure(e)
        }
    }
}