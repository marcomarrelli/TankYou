package project.unibo.tankyou.utils

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import project.unibo.tankyou.utils.Constants.AppLanguage
import java.util.Locale

/**
 * A `CompositionLocal` that holds the current [Vocabulary] instance.
 * This allows Composable functions to access localized strings using `LocalVocabulary.current`.
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
 * @property context The application context.
 * @property language The [AppLanguage] for which to provide strings.
 */
class Vocabulary(
    private val context: Context,
    private val language: AppLanguage
) {
    // Lazily create a context with the specified language configuration.
    private val localizedContext by lazy {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(language.code))
        context.createConfigurationContext(config)
    }

    /**
     * Retrieves a localized string for the given resource ID.
     * @param resId The resource ID of the string.
     * @return The localized string.
     */
    fun get(@StringRes resId: Int): String {
        return localizedContext.getString(resId)
    }
}

/**
 * A Composable function that provides the [Vocabulary] to its children.
 * It observes the current language from [SettingsManager] and updates the [Vocabulary] accordingly.
 * @param content The Composable content that will have access to the localized strings.
 */
@Composable
fun TankYouVocabulary(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by SettingsManager.currentLanguage

    // Remember the Vocabulary instance, recreating it if the language changes.
    val strings = remember(currentLanguage) {
        Vocabulary(context, currentLanguage)
    }

    CompositionLocalProvider(
        LocalVocabulary provides strings,
        content = content
    )
}

/**
 * A Composable function to conveniently access the current [Vocabulary] instance.
 * This is a shorthand for `LocalVocabulary.current`.
 * @return The current [Vocabulary] instance.
 */
@Composable
fun vocabulary(): Vocabulary {
    return LocalVocabulary.current
}

/**
 * A Composable function to directly retrieve a localized string using its resource ID.
 * This uses the [vocabulary] function to get the current [Vocabulary] and then retrieves the string.
 * @param id The resource ID of the string to retrieve.
 * @return The localized string.
 */
@Composable
fun getResourceString(@StringRes id: Int): String {
    return vocabulary().get(id)
}