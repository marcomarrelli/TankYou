package project.unibo.tankyou.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.User
import project.unibo.tankyou.data.database.entities.UserSavedGasStation
import project.unibo.tankyou.utils.Constants

/**
 * Repository class responsible for managing user-related data operations.
 * Handles user profiles, saved gas stations, and profile photo management.
 */
class UserRepository(private val supabase: SupabaseClient) {

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        /**
         * Returns the singleton instance of UserRepository.
         * Thread-safe implementation using double-checked locking pattern.
         *
         * @param supabaseClient The Supabase client instance to use
         *
         * @return The singleton instance of UserRepository
         */
        fun getInstance(supabaseClient: SupabaseClient = DatabaseClient.client): UserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserRepository(supabaseClient).also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieves the current authenticated user's profile from the users table.
     *
     * @return User object if found, null otherwise
     */
    suspend fun getCurrentUser(): User? {
        return try {
            Log.d(Constants.App.LOG_TAG, "Attempting to retrieve current user profile")

            if (!AuthRepository.getInstance().isUserLoggedIn()) {
                Log.w(Constants.App.LOG_TAG, "No user is currently logged in")
                return null
            }

            val authUser = AuthRepository.getInstance().currentUser ?: run {
                Log.w(Constants.App.LOG_TAG, "Auth user is null despite being logged in")
                return null
            }

            val user: User = supabase.from("users")
                .select {
                    filter {
                        eq("auth_user_id", authUser.id)
                    }
                }
                .decodeSingle<User>()

            Log.d(
                Constants.App.LOG_TAG,
                "Successfully retrieved user profile for auth user: ${authUser.id}"
            )

            user
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error retrieving current user profile", e)

            null
        }
    }

    /**
     * Retrieves all gas stations saved by the current user.
     *
     * @return List of saved gas stations, empty list if none found
     */
    suspend fun getUserSavedStations(): List<UserSavedGasStation> {
        return try {
            Log.d(Constants.App.LOG_TAG, "Fetching saved gas stations for current user")

            val currentUser: User? = getCurrentUser()
            currentUser?.let { user ->
                val savedStations: List<UserSavedGasStation> =
                    supabase.from("user_saved_gas_stations")
                        .select {
                            filter {
                                eq("user_id", user.id)
                            }
                        }
                        .decodeList<UserSavedGasStation>()

                Log.d(
                    Constants.App.LOG_TAG,
                    "Successfully retrieved ${savedStations.size} saved stations for user ${user.id}"
                )
                savedStations
            } ?: run {
                Log.w(Constants.App.LOG_TAG, "Cannot retrieve saved stations: current user is null")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error getting user saved stations", e)
            emptyList()
        }
    }

    /**
     * Saves a gas station for the current user.
     *
     * @param stationId The ID of the gas station to save
     * @param notes Optional notes about the gas station
     *
     * @return true if successful, false otherwise
     */
    suspend fun saveGasStation(stationId: Long, notes: String? = null): Boolean {
        return try {
            Log.d(
                Constants.App.LOG_TAG,
                "Attempting to save gas station $stationId with notes: ${notes?.let { "provided" } ?: "none"}"
            )

            val currentUser: User? = getCurrentUser()
            if (currentUser == null) {
                Log.w(Constants.App.LOG_TAG, "Cannot save gas station: user not logged in")
                return false
            }

            // Check if station is already saved
            val isAlreadySaved: Boolean = isGasStationSaved(stationId)
            if (isAlreadySaved) {
                Log.d(
                    Constants.App.LOG_TAG,
                    "Gas station $stationId is already saved for user ${currentUser.id}"
                )
                return true // Return true since the desired state is already achieved
            }

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
                Constants.App.LOG_TAG,
                "Successfully saved gas station $stationId for user ${currentUser.id}"
            )
            true
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error saving gas station $stationId", e)
            false
        }
    }

    /**
     * Removes a saved gas station for the current user.
     *
     * @param stationId The ID of the gas station to remove
     *
     * @return true if successful, false otherwise
     */
    suspend fun removeSavedGasStation(stationId: Long): Boolean {
        return try {
            Log.d(Constants.App.LOG_TAG, "Attempting to remove saved gas station $stationId")

            val currentUser: User? = getCurrentUser()
            currentUser?.let { user ->
                supabase.from("user_saved_gas_stations")
                    .delete {
                        filter {
                            eq("user_id", user.id)
                            eq("station_id", stationId)
                        }
                    }
                Log.d(
                    Constants.App.LOG_TAG,
                    "Successfully removed gas station $stationId for user ${user.id}"
                )
                true
            } ?: run {
                Log.w(Constants.App.LOG_TAG, "Cannot remove gas station: user not logged in")
                false
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error removing gas station $stationId", e)
            false
        }
    }

    /**
     * Checks if a gas station is saved by the current user.
     *
     * @param stationId The ID of the gas station to check
     *
     * @return true if saved, false otherwise
     */
    suspend fun isGasStationSaved(stationId: Long): Boolean {
        return try {
            Log.d(Constants.App.LOG_TAG, "Checking if gas station $stationId is saved")

            val currentUser: User? = getCurrentUser()
            currentUser?.let { user ->
                val result: List<UserSavedGasStation> = supabase.from("user_saved_gas_stations")
                    .select {
                        filter {
                            eq("user_id", user.id)
                            eq("station_id", stationId)
                        }
                        limit(1)
                    }
                    .decodeList<UserSavedGasStation>()

                val isSaved: Boolean = result.isNotEmpty()
                Log.d(
                    Constants.App.LOG_TAG,
                    "Gas station $stationId is ${if (isSaved) "saved" else "not saved"} for user ${user.id}"
                )
                isSaved
            } ?: run {
                Log.w(Constants.App.LOG_TAG, "Cannot check saved status: user not logged in")
                false
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error checking if gas station $stationId is saved", e)
            false
        }
    }

    /**
     * Uploads a profile photo for the current user to cloud storage.
     *
     * @param uri The URI of the image to upload
     * @param context The Android context for accessing content resolver
     *
     * @return The public URL of the uploaded image
     * @throws Exception if upload fails or user is not authenticated
     */
    suspend fun uploadProfilePhoto(uri: Uri, context: Context): String {
        return try {
            Log.d(Constants.App.LOG_TAG, "Starting profile photo upload process")

            val authUser = AuthRepository.getInstance().currentUser

            if (authUser == null) {
                Log.e(Constants.App.LOG_TAG, "User not authenticated for profile photo upload")
                throw Exception("User not logged in")
            }

            Log.d(Constants.App.LOG_TAG, "Auth user found for upload: ${authUser.id}")

            val fileName = "${authUser.id}/profile_${System.currentTimeMillis()}.jpg"
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open image file")

            val bytes: ByteArray = inputStream.readBytes()
            inputStream.close()

            Log.d(
                Constants.App.LOG_TAG,
                "Uploading profile photo with filename: $fileName, size: ${bytes.size} bytes"
            )

            supabase.storage
                .from("profile-photos")
                .upload(fileName, bytes)

            val publicUrl: String = supabase.storage
                .from("profile-photos")
                .publicUrl(fileName)

            Log.d(Constants.App.LOG_TAG, "Successfully uploaded profile photo: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error uploading profile photo", e)
            throw e
        }
    }

    /**
     * Updates user information in the database.
     *
     * @param user The updated user object
     *
     * @return true if successful, false otherwise
     */
    suspend fun updateUser(user: User): Boolean {
        return try {
            Log.d(Constants.App.LOG_TAG, "Updating user information for user ID: ${user.id}")

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

            Log.d(Constants.App.LOG_TAG, "Successfully updated user ${user.id}")
            true
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error updating user ${user.id}", e)
            false
        }
    }
}