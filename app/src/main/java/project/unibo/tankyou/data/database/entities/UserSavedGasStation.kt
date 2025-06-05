package project.unibo.tankyou.data.database.entities

data class UserSavedGasStation(
    val id: Int,
    val userId: Int,
    val stationId: Long,
    val savedAt: String,
    val notes: String?
)