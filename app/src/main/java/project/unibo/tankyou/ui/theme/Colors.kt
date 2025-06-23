package project.unibo.tankyou.ui.theme

import android.util.Log
import androidx.compose.ui.graphics.Color
import project.unibo.tankyou.utils.Constants

/**
 * Object defining the color palette for the light theme of the application.
 * Each color is represented as a Color object with proper type safety.
 *
 * This object provides a comprehensive set of colors for light mode UI elements,
 * including basic colors, disabled states, branding colors, and semantic status colors.
 */
object LightColors {

    /**
     * The color for titles in light theme.
     *
     * @return [Color] representing the title color
     */
    val Title: Color = Color(0xFF131101)

    /**
     * The color for regular text in light theme.
     *
     * @return [Color] representing the text color
     */
    val Text: Color = Color(0xFF1D1D1D)

    /**
     * The background color for most screens in light theme.
     *
     * @return [Color] representing the background color
     */
    val Background: Color = Color(0xFFDAE7CA)

    /**
     * The color for borders and dividers in light theme.
     *
     * @return [Color] representing the border color
     */
    val Border: Color = Color(0xFF8D9C7D)

    /**
     * The color for disabled text in light theme.
     *
     * @return [Color] representing the disabled text color
     */
    val DisabledText: Color = Color(0xFF7A7A7A)

    /**
     * The background color for disabled elements in light theme.
     *
     * @return [Color] representing the disabled background color
     */
    val DisabledBackground: Color = Color(0xFFE8F0DC)

    /**
     * The border color for disabled elements in light theme.
     *
     * @return [Color] representing the disabled border color
     */
    val DisabledBorder: Color = Color(0xFFD0DDBE)

    /**
     * The primary color, used for main actions and highlights in light theme.
     *
     * @return [Color] representing the primary color
     */
    val Primary: Color = Color(0xFFB6C892)

    /**
     * The secondary color, used for complementary actions and elements in light theme.
     *
     * @return [Color] representing the secondary color
     */
    val Secondary: Color = Color(0xFFA06522)

    /**
     * The accent color, used for drawing attention to specific elements in light theme.
     *
     * @return [Color] representing the accent color
     */
    val Accent: Color = Color(0xFFDA8A44)

    /**
     * The color for success or positive feedback (used also for cluster fullness) in light theme.
     *
     * @return [Color] representing the OK/success color
     */
    val OK: Color = Color(0xFF23BA39)

    /**
     * The color for warnings or cautionary messages (used also for cluster fullness) in light theme.
     *
     * @return [Color] representing the warning color
     */
    val Warning: Color = Color(0xFFEAAD16)

    /**
     * The color for alerts or error messages (used also for cluster fullness) in light theme.
     *
     * @return [Color] representing the alert/error color
     */
    val Alert: Color = Color(0xFFEC2E23)

    /**
     * A standard white color for light theme.
     *
     * @return [Color] representing white
     */
    val White: Color = Color(0xFFFEFAE1)

    /**
     * A standard black color for light theme.
     *
     * @return [Color] representing black
     */
    val Black: Color = Color(0xFF1E1A01)

    init {
        Log.i(Constants.App.LOG_TAG, "LightColors object initialized successfully")
    }
}

/**
 * Object defining the color palette for the dark theme of the application.
 * Each color is represented as a Color object with proper type safety.
 *
 * This object provides a comprehensive set of colors for dark mode UI elements,
 * including basic colors, disabled states, branding colors, and semantic status colors.
 */
object DarkColors {

    /**
     * The color for titles in dark theme.
     *
     * @return [Color] representing the title color
     */
    val Title: Color = Color(0xFFF8F3EC)

    /**
     * The color for regular text in dark theme.
     *
     * @return [Color] representing the text color
     */
    val Text: Color = Color(0xFFFEFAE1)

    /**
     * The background color for most screens in dark theme.
     *
     * @return [Color] representing the background color
     */
    val Background: Color = Color(0xFF283518)

    /**
     * The color for borders and dividers in dark theme.
     *
     * @return [Color] representing the border color
     */
    val Border: Color = Color(0xFF3A521F)

    /**
     * The color for disabled text in dark theme.
     *
     * @return [Color] representing the disabled text color
     */
    val DisabledText: Color = Color(0xFF8A8A8A)

    /**
     * The background color for disabled elements in dark theme.
     *
     * @return [Color] representing the disabled background color
     */
    val DisabledBackground: Color = Color(0xFF435037)

    /**
     * The border color for disabled elements in dark theme.
     *
     * @return [Color] representing the disabled border color
     */
    val DisabledBorder: Color = Color(0xFF242F18)

    /**
     * The primary color, used for main actions and highlights in dark theme.
     *
     * @return [Color] representing the primary color
     */
    val Primary: Color = Color(0xFF5B6D37)

    /**
     * The secondary color, used for complementary actions and elements in dark theme.
     *
     * @return [Color] representing the secondary color
     */
    val Secondary: Color = Color(0xFFDDA25F)

    /**
     * The accent color, used for drawing attention to specific elements in dark theme.
     *
     * @return [Color] representing the accent color
     */
    val Accent: Color = Color(0xFFBB6B25)

    /**
     * The color for success or positive feedback (used also for cluster fullness) in dark theme.
     *
     * @return [Color] representing the OK/success color
     */
    val OK: Color = Color(0xFF1A8A2B)

    /**
     * The color for warnings or cautionary messages (used also for cluster fullness) in dark theme.
     *
     * @return [Color] representing the warning color
     */
    val Warning: Color = Color(0xFFB8850F)

    /**
     * The color for alerts or error messages (used also for cluster fullness) in dark theme.
     *
     * @return [Color] representing the alert/error color
     */
    val Alert: Color = Color(0xFFB8211A)

    /**
     * A standard white color for dark theme.
     *
     * @return [Color] representing white
     */
    val White: Color = Color(0xFFFEFAE1)

    /**
     * A standard black color for dark theme.
     *
     * @return [Color] representing black
     */
    val Black: Color = Color(0xFF1E1A01)

    init {
        Log.i(Constants.App.LOG_TAG, "DarkColors object initialized successfully")
    }
}