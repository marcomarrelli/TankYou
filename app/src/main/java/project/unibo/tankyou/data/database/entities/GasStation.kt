package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gas Station Database Table/Entity
 */
@Serializable
data class GasStation(
    /** Gas Station ID */
    @SerialName("id")
    val id: Long,

    /** Gas Station Owner */
    @SerialName("owner")
    val owner: String? = "N/A",

    /** Gas Station Flag Reference ID */
    @SerialName("flag")
    val flag: Int,

    /** Gas Station Type Reference ID */
    @SerialName("type")
    val type: Int,

    /** Gas Station Name */
    @SerialName("name")
    val name: String? = "N/A",

    /** Gas Station Address */
    @SerialName("address")
    val address: String? = "N/A",

    /** Gas Station City */
    @SerialName("city")
    val city: String? = "N/A",

    /** Gas Station Province */
    @SerialName("province")
    val province: String? = "N/A",

    /** Gas Station Latitude */
    @SerialName("latitude")
    val latitude: Double,

    /** Gas Station Longitude */
    @SerialName("longitude")
    val longitude: Double
)