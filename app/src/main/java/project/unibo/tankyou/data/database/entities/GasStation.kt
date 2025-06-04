package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.Serializable

/**
 * Gas Station Database Table/Entity
 */
@Serializable
data class GasStation(
    /** Gas Station ID */
    val id: Long,

    /** Gas Station Owner */
    val owner: String? = "N/A",

    /** Gas Station Flag Reference ID */
    val flag: Int,

    /** Gas Station Type Reference ID */
    val type: Int,

    /** Gas Station Name */
    val name: String? = "N/A",

    /** Gas Station Address */
    val address: String? = "N/A",

    /** Gas Station City */
    val city: String? = "N/A",

    /** Gas Station Province */
    val province: String? = "N/A",

    /** Gas Station Latitude */
    val latitude: Double,

    /** Gas Station Longitude */
    val longitude: Double
)