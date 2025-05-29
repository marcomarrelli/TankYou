package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.Serializable

@Serializable
data class GasStation(
    val id: Long,
    val owner: String,
    val flag: Int,
    val type: Int,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val latitude: Double,
    val longitude: Double
)