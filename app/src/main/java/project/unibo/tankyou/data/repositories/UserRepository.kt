package project.unibo.tankyou.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
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
            if (!AuthRepository.getInstance().isUserLoggedIn()) {
                return null
            }

            val authUser = AuthRepository.getInstance().currentUser ?: return null

            supabase.from("users")
                .select() {
                    filter {
                        eq("auth_user_id", authUser.id)
                    }
                }
                .decodeSingle<User>()
        } catch (e: Exception) {
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
            Log.e("UserRepository", "Error getting user saved stations", e)
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
            if (currentUser == null) {
                Log.w("UserRepository", "Cannot save gas station: user not logged in")
                return false
            }

            // Check if station is already saved
            val isAlreadySaved = isGasStationSaved(stationId)
            if (isAlreadySaved) {
                Log.i(
                    "UserRepository",
                    "Gas station $stationId is already saved for user ${currentUser.id}"
                )
                return true // Return true since the desired state is already achieved
            }

            // @Deprecated
            // put("saved_at", now) > Not Needed because Database has default = now()
            // val now = Calendar.getInstance().toDate(Calendar.getInstance().timeInMillis).toHttpDate()

            supabase.from("user_saved_gas_stations")
                .insert(
                    buildJsonObject {
                        put("user_id", currentUser.id)
                        put("station_id", stationId)
                        if (notes != null) {
                            put("notes", notes)
                        }
                    }
                )

            Log.d(
                "UserRepository",
                "Successfully saved gas station $stationId for user ${currentUser.id}"
            )
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving gas station $stationId", e)
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
                Log.d(
                    "UserRepository",
                    "Successfully removed gas station $stationId for user ${user.id}"
                )
                true
            } ?: run {
                Log.w("UserRepository", "Cannot remove gas station: user not logged in")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing gas station $stationId", e)
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
                        limit(1)
                    }
                    .decodeList<UserSavedGasStation>()

                result.isNotEmpty()
            } ?: false
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking if gas station $stationId is saved", e)
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
            Log.e("UserRepository", "Error getting ordered saved stations", e)
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
                Log.d("UserRepository", "Successfully updated notes for gas station $stationId")
                true
            } ?: run {
                Log.w("UserRepository", "Cannot update notes: user not logged in")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating notes for gas station $stationId", e)
            false
        }
    }

    suspend fun uploadProfilePhoto(uri: Uri, context: Context): String {
        return try {
            var authUser = AuthRepository.getInstance().currentUser

            if (authUser == null) {
                Log.e("UserRepository", "User not authenticated after retries")
                throw Exception("User not logged in")
            }

            Log.d("UserRepository", "Auth user found: ${authUser.id}")

            val fileName = "${authUser.id}/profile_${System.currentTimeMillis()}.jpg"
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open image file")

            val bytes = inputStream.readBytes()
            inputStream.close()

            supabase.storage
                .from("profile-photos")
                .upload(fileName, bytes)

            val publicUrl = supabase.storage
                .from("profile-photos")
                .publicUrl(fileName)

            Log.d("UserRepository", "Successfully uploaded profile photo: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("UserRepository", "Error uploading profile photo", e)
            throw e
        }
    }

    /**
     * Updates user information in the database
     * @param user The updated user object
     * @return true if successful, false otherwise
     */
    suspend fun updateUser(user: User): Boolean {
        return try {
            supabase.from("users")
                .update(
                    buildJsonObject {
                        put("name", user.name)
                        put("surname", user.surname)
                        put("username", user.username)
                        put("email", user.email)
                        user.profilePicture?.let { put("profile_picture", it) }
                    }
                ) {
                    filter {
                        eq("id", user.id)
                    }
                }

            Log.d("UserRepository", "Successfully updated user ${user.id}")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user ${user.id}", e)
            false
        }
    }

    suspend fun updateUserProfilePhoto(userId: Int, photoUrl: String): Boolean {
        return try {
            supabase.from("users")
                .update(mapOf("profile_picture" to photoUrl)) {
                    filter {
                        eq("id", userId)
                    }
                }
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user profile photo", e)
            false
        }
    }
}