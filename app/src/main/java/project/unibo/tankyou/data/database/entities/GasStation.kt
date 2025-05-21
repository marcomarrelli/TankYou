package project.unibo.tankyou.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import android.util.Log

@Entity(tableName = "gas_stations")
data class GasStation(
    @PrimaryKey
    val id: String,
    val owner: String,
    val company: String,
    val type: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun fromCsvRow(row: Array<String>): GasStation? {
            return try {
                GasStation(
                    id = row[0].trim(),
                    owner = row[1].trim(),
                    company = row[2].trim(),
                    type = row[3].trim(),
                    name = row[4].trim(),
                    address = row[5].trim(),
                    city = row[6].trim(),
                    province = row[7].trim(),
                    latitude = row[8].trim().toDouble(),
                    longitude = row[9].trim().toDouble()
                )
            }
            catch (e: Exception) {
                Log.w("Failed to Fetch Gas Station:", e)
                null
            }
        }
    }
}