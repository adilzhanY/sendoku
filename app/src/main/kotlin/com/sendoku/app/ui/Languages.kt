package com.sendoku.app.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.sendoku.app.R

/**
 * The languages Sendoku is written in.
 *
 * The tag is what Android wants and the name is what a person wants, written in the
 * language itself. A picker that offers "German" to somebody who only reads German has
 * failed at the one job a language picker has.
 */
public enum class Language(public val tag: String, @StringRes public val label: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    RUSSIAN("ru", R.string.language_russian),
    GERMAN("de", R.string.language_german),
    TURKISH("tr", R.string.language_turkish),
    SPANISH("es", R.string.language_spanish),
}

/**
 * Switching language without restarting anything the player can see.
 *
 * On Android 13 and later this hands the choice to the system, which stores it per app and
 * shows it in the settings app alongside every other application. Below that there is no
 * such thing, so AppCompat keeps the choice itself and applies it at launch, which is what
 * the service in the manifest is for.
 *
 * Either way it is one call. The alternative was to follow the system language and nothing
 * else, which would leave a Turkish speaker holding an English phone with an English sudoku
 * app and no way to change it.
 */
public object Languages {

    /** What the app is set to, or [Language.SYSTEM] when it is following the phone. */
    public fun current(): Language {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
        return Language.entries.firstOrNull { it.tag.isNotEmpty() && it.tag == tag } ?: Language.SYSTEM
    }

    public fun choose(language: Language) {
        AppCompatDelegate.setApplicationLocales(
            if (language == Language.SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.tag)
            },
        )
    }
}
