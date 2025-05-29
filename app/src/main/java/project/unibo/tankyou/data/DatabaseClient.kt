package project.unibo.tankyou.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import project.unibo.tankyou.BuildConfig

object DatabaseClient {
    private val SUPABASE_URL: String
        get() = if (BuildConfig.DATABASE_URL.isNotBlank()) {
            BuildConfig.DATABASE_URL
        } else {
            throw IllegalStateException("DATABASE_URL is not configured")
        }

    private val SUPABASE_ANON_KEY: String
        get() = if (BuildConfig.DATABASE_KEY.isNotBlank()) {
            BuildConfig.DATABASE_KEY
        } else {
            throw IllegalStateException("DATABASE_KEY is not configured")
        }

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
    }
}