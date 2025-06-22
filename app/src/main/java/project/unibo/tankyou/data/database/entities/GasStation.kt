package project.unibo.tankyou.data.database.entities

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import project.unibo.tankyou.R
import project.unibo.tankyou.utils.Constants.GAS_STATION_FLAGS
import project.unibo.tankyou.utils.Constants.GAS_STATION_TYPES
import project.unibo.tankyou.utils.getResourceString

/**
 * Gas Station Type Database Table/Entity
 */
@Serializable
data class GasStationType(
    /** Gas Station Type ID */
    @SerialName("id")
    val id: Int,

    /** Gas Station Type Name */
    @SerialName("name")
    val name: String
)

/**
 * Gas Station Flag Database Table/Entity
 */
@Serializable
data class GasStationFlag(
    /** Gas Station Flag ID */
    @SerialName("id")
    val id: Int,

    /** Gas Station Flag Name */
    @SerialName("name")
    val name: String
)

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

/**
 * Gets a fuel type name by its ID, with fallback to "Unknown Fuel"
 */
@Composable
fun Int.toTypeLabel(): String {
    val id: Int = this
    return when (GAS_STATION_TYPES.find { it.id == id }?.name?.lowercase()) {
        "r" -> getResourceString(R.string.gas_station_type_r)
        "h" -> getResourceString(R.string.gas_station_type_h)
        else -> getResourceString(R.string.not_available)
    }
}

/**
 * Gets a gas station flag name by its ID, with fallback to "Independent"
 */
@Composable
fun Int.toFlagLabel(): String {
    val id: Int = this
    return GAS_STATION_FLAGS.find { it.id == id }?.name
        ?: getResourceString(R.string.not_available)
}