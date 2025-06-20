package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: Int,

    @SerialName("auth_user_id")
    val authUserId: String,

    @SerialName("name")
    val name: String,

    @SerialName("surname")
    val surname: String,

    @SerialName("username")
    val username: String,

    @SerialName("email")
    val email: String,

    @SerialName("profile_picture")
    val profilePicture: String? = ""
)