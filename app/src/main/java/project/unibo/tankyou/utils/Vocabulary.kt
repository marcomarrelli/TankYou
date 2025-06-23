package project.unibo.tankyou.utils

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import project.unibo.tankyou.utils.Constants.App.LOG_TAG
import project.unibo.tankyou.utils.Constants.AppLanguage
import java.util.Locale

/**
 * A CompositionLocal that holds the current [Vocabulary] instance.
 *
 * This allows Composable functions to access localized strings using [LocalVocabulary.current][vocabulary].
 * An error is thrown if this is accessed before being provided by [TankYouVocabulary].
 */
val LocalVocabulary = compositionLocalOf<Vocabulary> {
    error(
        "LocalStrings not provided - wrap your content with ProvideLocalizedStrings"
    )
}

/**
 * Manages localized strings for a specific language.
 *
 * Creates a localized context based on the specified language and provides
 * methods to retrieve localized string resources. The localized context
 * is created lazily to optimize performance.
 *
 * @property context The application context
 * @property language The AppLanguage for which to provide strings
 */
class Vocabulary(
    private val context: Context,
    private val language: AppLanguage
) {
    // Lazily create a context with the specified language configuration.
    private val localizedContext: Context by lazy {
        Log.d(LOG_TAG, "Creating localized context for language: ${language.code}")
        try {
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale(language.code))
            val localizedCtx: Context = context.createConfigurationContext(config)
            Log.d(LOG_TAG, "Localized context created successfully")
            localizedCtx
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error creating localized context, falling back to original", e)
            context
        }
    }

    /**
     * Retrieves a localized string for the given resource ID.
     *
     * @param resId The resource ID of the string
     *
     * @return The localized string
     */
    fun get(@StringRes resId: Int): String {
        Log.d(LOG_TAG, "Getting localized string for resource ID: $resId")
        return try {
            val localizedString: String = localizedContext.getString(resId)
            Log.d(LOG_TAG, "Successfully retrieved localized string")
            localizedString
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error retrieving localized string for resource ID: $resId", e)
            "String not found"
        }
    }
}

/**
 * A Composable function that provides the [Vocabulary] to its children.
 *
 * It observes the current language from [SettingsManager] and updates the [Vocabulary] accordingly.
 * The [Vocabulary] instance is remembered and recreated only when the language changes.
 *
 * @param content The [Composable] content that will have access to the localized strings
 */
@Composable
fun TankYouVocabulary(
    content: @Composable () -> Unit
) {
    val context: Context = LocalContext.current
    val currentLanguage: AppLanguage by SettingsManager.currentLanguage

    // Remember the Vocabulary instance, recreating it if the language changes.
    val strings: Vocabulary = remember(currentLanguage) {
        Log.d(LOG_TAG, "Creating Vocabulary instance for language: ${currentLanguage.code}")
        Vocabulary(context, currentLanguage)
    }

    CompositionLocalProvider(
        LocalVocabulary provides strings,
        content = content
    )
}

/**
 * A Composable function to conveniently access the current [Vocabulary] instance.
 *
 * This is a shorthand for LocalVocabulary.current and provides easy access
 * to the vocabulary from any [Composable] within the [TankYouVocabulary] scope.
 *
 * @return The current [Vocabulary] instance
 */
@Composable
fun vocabulary(): Vocabulary {
    return LocalVocabulary.current
}

/**
 * A Composable function to directly retrieve a localized string using its resource ID.
 *
 * This uses the vocabulary function to get the current Vocabulary and then retrieves the string.
 * It's a convenience function for quickly accessing localized strings in [Composables][Composable].
 *
 * @param id The resource ID of the string to retrieve
 *
 * @return The localized string
 */
@Composable
fun getResourceString(@StringRes id: Int): String {
    return vocabulary().get(id)
}