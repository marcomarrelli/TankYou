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

val LocalVocabulary = compositionLocalOf<Vocabulary> {
    error(
        "LocalStrings not provided - wrap your content with ProvideLocalizedStrings"
    )
}

class Vocabulary(
    private val context: Context,
    private val language: AppLanguage
) {
    private val localizedContext by lazy {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(language.code))
        context.createConfigurationContext(config)
    }

    /**
     * Ottiene una stringa localizzata dal resource ID
     */
    fun get(@StringRes resId: Int): String {
        return localizedContext.getString(resId)
    }
}

/**
 * Provider Composable che fornisce le stringhe localizzate a tutti i componenti figli
 */
@Composable
fun TankYouVocabulary(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by SettingsManager.currentLanguage

    val strings = remember(currentLanguage) {
        Vocabulary(context, currentLanguage)
    }

    CompositionLocalProvider(
        LocalVocabulary provides strings,
        content = content
    )
}

/**
 * Hook per accedere alle stringhe localizzate in qualsiasi Composable
 */
@Composable
fun vocabulary(): Vocabulary {
    return LocalVocabulary.current
}

@Composable
fun getResourceString(@StringRes id: Int): String {
    return vocabulary().get(id)
}