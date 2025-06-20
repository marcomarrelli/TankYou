package project.unibo.tankyou.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import project.unibo.tankyou.BuildConfig

/**
 * Singleton object that provides a configured Supabase client instance for database operations.
 * Handles client initialization with proper configuration validation and module installation.
 */
object DatabaseClient {
    /**
     * The Supabase database URL retrieved from configuration.
     * Validates that the URL is properly configured before returning it.
     *
     * @throws IllegalStateException if [BuildConfig.DATABASE_URL] is not configured or blank
     */
    private val SUPABASE_URL: String
        get() = if (BuildConfig.DATABASE_URL.isNotBlank()) {
            BuildConfig.DATABASE_URL
        } else {
            throw IllegalStateException("DATABASE_URL is not configured")
        }

    /**
     * The Supabase anonymous key retrieved from [build configuration][BuildConfig]
     * Validates that the [key][BuildConfig.DATABASE_KEY] is properly configured before returning it.
     *
     * @throws IllegalStateException if [BuildConfig.DATABASE_KEY] is not configured or blank
     */
    private val SUPABASE_ANON_KEY: String
        get() = if (BuildConfig.DATABASE_KEY.isNotBlank()) {
            BuildConfig.DATABASE_KEY
        } else {
            throw IllegalStateException("DATABASE_KEY is not configured")
        }

    /**
     * The configured Supabase client instance with installed modules.
     * Includes Postgrest for database operations, Realtime for live updates, and Auth for authentication.
     * This client is used throughout the application for all database interactions.
     */
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
        install(Auth)
        install(Storage)
    }
}