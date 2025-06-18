package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val localizedFormat = when (Locale.getDefault().country) {
        "US" -> SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
        else -> SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    }

    return try {
        val cleanedString = this.replace("T", " ").substring(0, 19)
        val date = isoFormat.parse(cleanedString)
        localizedFormat.format(date ?: Date())
    } catch (e: Exception) {
        this.replace("T", " ")
    }
}