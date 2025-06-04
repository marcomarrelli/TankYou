package project.unibo.tankyou.data.database.auth

/**
 * Sealed class representing the different authentication states in the application.
 * This class provides a type-safe way to handle authentication status changes
 * and ensures exhaustive when expressions for state handling.
 */
sealed class AuthState {
    /**
     * Loading state during authentication operations.
     * Used when the app is checking authentication status or performing login/logout operations.
     */
    object Loading : AuthState()

    /**
     * Authenticated state when a user is successfully logged in.
     * Indicates that the user has valid credentials and can access protected features.
     */
    object Authenticated : AuthState()

    /**
     * Unauthenticated state when no user is logged in.
     * Indicates that the user needs to sign in to access protected features.
     */
    object Unauthenticated : AuthState()

    /**
     * Error state during authentication operations.
     * Contains the error message to be displayed to the user.
     *
     * @param message the error message describing what went wrong during authentication
     */
    data class Error(val message: String) : AuthState()
}