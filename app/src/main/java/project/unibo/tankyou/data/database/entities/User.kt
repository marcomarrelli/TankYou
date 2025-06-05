package project.unibo.tankyou.data.database.entities

data class User(
    val id: Int,
    val authUserId: String,
    val name: String,
    val surname: String,
    val username: String,
    val email: String
)