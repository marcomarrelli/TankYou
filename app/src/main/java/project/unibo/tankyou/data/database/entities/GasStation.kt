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
    val owner: String?,

    /** Gas Station GasStationFlag Reference ID */
    @SerialName("flag")
    val flag: Int,

    /** Gas Station Type Reference ID */
    @SerialName("type")
    val type: Int,

    /** Gas Station Name */
    @SerialName("name")
    val name: String?,

    /** Gas Station Address */
    @SerialName("address")
    val address: String?,

    /** Gas Station City */
    @SerialName("city")
    val city: String?,

    /** Gas Station Province */
    @SerialName("province")
    val province: String?,

    /** Gas Station Latitude */
    @SerialName("latitude")
    val latitude: Double,

    /** Gas Station Longitude */
    @SerialName("longitude")
    val longitude: Double
)