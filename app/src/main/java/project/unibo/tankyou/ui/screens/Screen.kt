package project.unibo.tankyou.ui.screens

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Login : Screen()

    @Serializable
    data object Register : Screen()

    @Serializable
    data object Map : Screen()

    @Serializable
    data object Profile : Screen()

    @Serializable
    data object Settings : Screen()
}