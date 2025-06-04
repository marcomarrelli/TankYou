package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.Serializable

/**
 * Fuel Database Table/Entity
 */
@Serializable
data class Fuel(
    /** [GasStation] Reference ID */
    val stationId: Int,

    /** Fuel Type Reference ID */
    val type: Int,

    /** Fuel Price */
    val price: Double,

    /** If Fuel is Self Erogated */
    val self: Boolean,

    /** Date and Time (YYYY/MM/DD hh:mm:ss) of Price Logging */
    val date: String
)