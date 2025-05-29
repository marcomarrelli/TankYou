package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.Serializable

@Serializable
data class GasStation(
    val id: Long,
    val owner: String? = "N/A",
    val flag: Int,
    val type: Int,
    val name: String? = "N/A",
    val address: String? = "N/A",
    val city: String? = "N/A",
    val province: String? = "N/A",
    val latitude: Double,
    val longitude: Double
)