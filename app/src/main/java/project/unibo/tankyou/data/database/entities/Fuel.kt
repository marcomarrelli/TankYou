package project.unibo.tankyou.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import android.util.Log

@Entity(
    tableName = "fuels",
    foreignKeys = [
        ForeignKey(
            entity = GasStation::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["stationID", "type", "self"], unique = true)
    ]
)
data class Fuel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stationID: String,
    val type: String,
    val price: Double,
    val self: Boolean,
    val date: LocalDateTime
) {
    companion object {
        fun fromCsvRow(row: Array<String>): Fuel? {
            return try {
                Fuel(
                    stationID = row[0].trim(),
                    type = row[1].trim(),
                    price = row[2].trim().toDouble(),
                    self = row[3].trim().toInt() == 1,
                    date = LocalDateTime.parse(row[4].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                )
            } catch (e: Exception) {
                Log.w("Failed to Fetch Fuel:", e)
                null
            }
        }
    }
}