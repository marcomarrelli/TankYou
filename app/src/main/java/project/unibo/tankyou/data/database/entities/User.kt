package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User Database Table/Entity
 */
@Serializable
data class User(
    /** User ID */
    @SerialName("id")
    val id: Int,

    /** User Authentication ID - Supabase */
    @SerialName("auth_user_id")
    val authUserId: String,

    /** User First Name */
    @SerialName("name")
    val name: String,

    /** User Last Name */
    @SerialName("surname")
    val surname: String,

    /** User Username */
    @SerialName("username")
    val username: String,

    /** User Email */
    @SerialName("email")
    val email: String,

    /** User Profile Picture */
    @SerialName("profile_picture")
    val profilePicture: String? = ""
)