package project.unibo.tankyou.ui.screens

import project.unibo.tankyou.ui.screens.Screen.LOGIN
import project.unibo.tankyou.ui.screens.Screen.MAP
import project.unibo.tankyou.ui.screens.Screen.PROFILE
import project.unibo.tankyou.ui.screens.Screen.REGISTER
import project.unibo.tankyou.ui.screens.Screen.SETTINGS

/**
 * Enum representing the different screens in the application.
 *
 * @param LOGIN User Login Page
 * @param REGISTER User Registration Page
 * @param PROFILE User Profile Page
 * @param MAP Map Page
 * @param SETTINGS Settings Page
 */
enum class Screen {
    /** User Login Page */
    LOGIN,

    /** User Registration Page */
    REGISTER,

    /** User Profile Page */
    PROFILE,

    /** Map Page */
    MAP,

    /** Settings Page */
    SETTINGS
}