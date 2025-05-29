package project.unibo.tankyou.data.database.entities

import kotlinx.serialization.Serializable

@Serializable
data class Fuel(
    val stationId: Int,
    val type: Int,
    val price: Double,
    val self: Boolean,
    val date: String
)