package project.unibo.tankyou.data.repositories

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.User
import project.unibo.tankyou.data.database.entities.UserSavedGasStation

class UserRepository(private val supabase: SupabaseClient) {

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(supabaseClient: SupabaseClient = DatabaseClient.client): UserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserRepository(supabaseClient).also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieves the current authenticated user's profile from the users table
     * @return User object if found, null otherwise
     */
    suspend fun getCurrentUser(): User? {
        return try {
            val authUser = supabase.auth.retrieveUser("") // Nessun parametro richiesto
            authUser.let {
                supabase.from("users")
                    .select() {
                        filter {
                            eq("auth_user_id", it.id)
                        }
                    }
                    .decodeSingle<User>()
            }
        } catch (e: Exception) {
            e
            null
        }
    }

    /**
     * Retrieves all gas stations saved by the current user
     * @return List of saved gas stations, empty list if none found
     */
    suspend fun getUserSavedStations(): List<UserSavedGasStation> {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("user_saved_gas_stations")
                    .select() {
                        filter {
                            eq("user_id", user.id)
                        }
                    }
                    .decodeList<UserSavedGasStation>()
            } ?: emptyList()
        } catch (e: Exception) {
            e
            emptyList()
        }
    }

    /**
     * Saves a gas station for the current user
     * @param stationId The ID of the gas station to save
     * @param notes Optional notes about the gas station
     * @return true if successful, false otherwise
     */
    suspend fun saveGasStation(stationId: Long, notes: String? = null): Boolean {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("user_saved_gas_stations")
                    .insert(
                        buildJsonObject {
                            put("user_id", user.id)
                            put("station_id", stationId)
                            if (notes != null) {
                                put("notes", notes)
                            }
                        }
                    )
                true
            } == true
        } catch (e: Exception) {
            e
            false
        }
    }

    /**
     * Removes a saved gas station for the current user
     * @param stationId The ID of the gas station to remove
     * @return true if successful, false otherwise
     */
    suspend fun removeSavedGasStation(stationId: Long): Boolean {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("user_saved_gas_stations")
                    .delete {
                        filter {
                            eq("user_id", user.id)
                            eq("station_id", stationId)
                        }
                    }
                true
            } == true
        } catch (e: Exception) {
            e
            false
        }
    }

    /**
     * Checks if a gas station is saved by the current user
     * @param stationId The ID of the gas station to check
     * @return true if saved, false otherwise
     */
    suspend fun isGasStationSaved(stationId: Long): Boolean {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                val result = supabase.from("user_saved_gas_stations")
                    .select {
                        filter {
                            eq("user_id", user.id)
                            eq("station_id", stationId)
                        }
                    }
                    .decodeList<Map<String, Any>>()
                result.isNotEmpty()
            } == true
        } catch (e: Exception) {
            e
            false
        }
    }

    /**
     * Updates user profile information
     * @param name User's first name
     * @param surname User's surname
     * @param username User's username
     * @return true if successful, false otherwise
     */
    suspend fun updateUserProfile(name: String, surname: String, username: String): Boolean {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("users")
                    .update(
                        buildJsonObject {
                            put("name", name)
                            put("surname", surname)
                            put("username", username)
                        }
                    ) {
                        filter {
                            eq("id", user.id)
                        }
                    }
                true
            } == true
        } catch (e: Exception) {
            e
            false
        }
    }

    /**
     * Gets user saved stations with details ordered by save date
     * @return List of saved gas stations ordered by most recent
     */
    suspend fun getUserSavedStationsOrdered(): List<UserSavedGasStation> {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("user_saved_gas_stations")
                    .select {
                        filter {
                            eq("user_id", user.id)
                        }
                        order("saved_at", Order.DESCENDING)
                    }
                    .decodeList<UserSavedGasStation>()
            } ?: emptyList()
        } catch (e: Exception) {
            e
            emptyList()
        }
    }

    /**
     * Updates notes for a saved gas station
     * @param stationId The ID of the gas station
     * @param notes New notes for the station
     * @return true if successful, false otherwise
     */
    suspend fun updateGasStationNotes(stationId: Long, notes: String): Boolean {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("user_saved_gas_stations")
                    .update(
                        buildJsonObject {
                            put("notes", notes)
                        }
                    ) {
                        filter {
                            eq("user_id", user.id)
                            eq("station_id", stationId)
                        }
                    }
                true
            } == true
        } catch (e: Exception) {
            e
            false
        }
    }
}