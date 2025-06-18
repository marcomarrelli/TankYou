package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSavedGasStation(
    @SerialName("id")
    val id: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("station_id")
    val stationId: Long,

    @SerialName("saved_at")
    val savedAt: String,

    @SerialName("notes")
    val notes: String?
)