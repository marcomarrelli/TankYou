package project.unibo.tankyou.data.database.entities

import androidx.compose.runtime.Composable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import project.unibo.tankyou.R
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.Constants.FUEL_TYPES
import project.unibo.tankyou.utils.SettingsManager
import project.unibo.tankyou.utils.getResourceString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class FuelType(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String
)

/**
 * Fuel Database Table/Entity
 */
@Serializable
data class Fuel(
    /** [GasStation] Reference ID */
    @SerialName("station_id")
    val stationId: Int,

    /** Fuel Type Reference ID */
    @SerialName("type")
    val type: Int,

    /** Fuel Price */
    @SerialName("price")
    val price: Double,

    /** If Fuel is Self Erogated */
    @SerialName("self")
    val self: Boolean,

    /**
     * Date and Time (YYYY/MM/DD hh:mm:ss) of Price Logging
     *
     * @param date Receiving YYYY/MM/DD'T'hh:mm:ss from Postgres Database! Please Convert It
     *
     * @see toLocalizedDateFormat
     */
    @SerialName("last_update")
    val date: String
)

/**
 * Postgres Date Format to Localized Date Format
 *
 * Converts YYYY/MM/DD'T'hh:mm:ss to MM/DD/YYYY hh:mm:ss or dd/MM/YYYY hh:mm:ss depending on the User Locale
 */
fun String.toLocalizedDateFormat(): String {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val localizedFormat = when (SettingsManager.currentLanguage.value) {
        Constants.AppLanguage.ENGLISH -> SimpleDateFormat(
            "MM/dd/yyyy HH:mm:ss",
            Locale.getDefault()
        )

        Constants.AppLanguage.ITALIAN -> SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        )
    }

    return try {
        val date = isoFormat.parse(this)
        localizedFormat.format(date ?: Date())
    } catch (e: Exception) {
        println("Error parsing date: $this - ${e.message}")
        this.replace("T", " ")
    }
}

/**
 * Converts the 'self' flag (boolean) to a Label String
 */
@Composable
fun Boolean.toLabel(): String {
    return if (this) getResourceString(R.string.self_service_label) else getResourceString(R.string.full_service_label)
}

/**
 * Gets a fuel type name by its ID, with fallback to "Unknown Fuel"
 */
@Composable
fun Int.toFuelTypeName(): String {
    val id = this
    return when (FUEL_TYPES.find { it.id == id }?.name?.lowercase()) {
        "g" -> getResourceString(R.string.fuel_type_g)
        "d" -> getResourceString(R.string.fuel_type_d)
        "c" -> getResourceString(R.string.fuel_type_c)
        "l" -> getResourceString(R.string.fuel_type_l)
        else -> getResourceString(R.string.not_available)
    }
}