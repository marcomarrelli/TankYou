package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User Database Table/Entity
 */
@Serializable
data class UserSavedGasStation(
    /** User Saved Gas Station ID */
    @SerialName("id")
    val id: Int,

    /** User ID */
    @SerialName("user_id")
    val userId: Int,

    /** Gas Station ID */
    @SerialName("station_id")
    val stationId: Long,

    /** Gas Station Name */
    @SerialName("saved_at")
    val savedAt: String,

    /** Gas Station Address */
    @SerialName("notes")
    val notes: String?
)