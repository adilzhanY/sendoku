package com.sendoku.app.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.sendoku.app.R
import java.util.Locale

/**
 * The languages Sendoku is written in.
 *
 * The tag is what Android wants and the name is what a person wants, written in the language
 * itself. A picker that offers "German" to somebody who only reads German has failed at the
 * one job a language picker has.
 */
public enum class Language(public val tag: String, @StringRes public val label: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    RUSSIAN("ru", R.string.language_russian),
    GERMAN("de", R.string.language_german),
    TURKISH("tr", R.string.language_turkish),
    SPANISH("es", R.string.language_spanish),
    ITALIAN("it", R.string.language_italian),
    JAPANESE("ja", R.string.language_japanese),
    FRENCH("fr", R.string.language_french),
    PORTUGUESE("pt", R.string.language_portuguese),

    /**
     * Simplified Chinese, and only Simplified.
     *
     * The tag carries the script because that is what is being promised. Somebody reading
     * Traditional in Taipei or Hong Kong is not served by being handed Simplified: it is a
     * different written language to read, and until there is a Traditional translation the
     * honest answer is the English they already had.
     */
    CHINESE("zh-Hans", R.string.language_chinese),
    KOREAN("ko", R.string.language_korean),

    /**
     * Arabic, with the numbering system named in the tag.
     *
     * Android formats numbers in the numbering system of the locale, and for Arabic that is
     * Arabic-Indic: ١ ٢ ٣ rather than 1 2 3. The board can only draw Western digits, and
     * mixing the two systems in one screen is the mistake an Arabic player notices first, so
     * the tag asks for Arabic with Latin digits. Resources still resolve on the language, so
     * this reads values-ar exactly as ar would.
     */
    ARABIC("ar-u-nu-latn", R.string.language_arabic),
}

/**
 * Switching the language of the app, and making it stick.
 *
 * This used to go through AppCompatDelegate, and it never worked. AppCompat applies a chosen
 * locale through its own activities, and this app's activity is a ComponentActivity, so the
 * choice was stored in memory, the radio button moved, and not one word on screen changed.
 * The system never heard about it either. Nobody noticed because every test set the language
 * with adb rather than by tapping the thing a player taps.
 *
 * So the app owns it now, in the two ways Android has.
 *
 * On Android 13 and later the system keeps the per app language itself. Setting it there is
 * what makes Sendoku appear in the phone's own per app language screen, and the system
 * restarts the activity and hands it the right resources.
 *
 * Below that there is no such thing, so the tag is kept in a preferences file of its own and
 * applied in attachBaseContext, before a single resource is read. It has to be its own file
 * rather than the DataStore everything else lives in: attaching a context happens before
 * anything can wait for a coroutine, and a language that arrives one frame late is a screen
 * drawn in the wrong one.
 */
public object Languages {

    private const val FILE = "language"
    private const val KEY = "tag"

    /** What the app is set to, or [Language.SYSTEM] when it is following the phone. */
    public fun current(context: Context): Language {
        val tag = stored(context)
        // Compared on the language alone, because that is all a stored locale carries back.
        // A tag with a script in it, like zh-Hans, still comes home as zh.
        return Language.entries.firstOrNull { it.tag.isNotEmpty() && it.tag.substringBefore('-') == tag }
            ?: Language.SYSTEM
    }

    /**
     * Chooses a language and applies it now.
     *
     * The activity goes away and comes back, which is what a language change is: every string
     * on screen has to be read again. On 13 and later the system does that itself once the
     * locale is set, so asking twice would be a visible double flash.
     */
    public fun choose(activity: Activity, language: Language) {
        activity.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY, language.tag) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language.tag.isEmpty()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(
                        language.tag,
                    )
                }
        } else {
            activity.recreate()
        }
    }

    /**
     * The context an activity should attach, with the chosen language already in it.
     *
     * A no-op on 13 and later, where the system has already handed over a context in the
     * right language, and on any version when the player is following the phone.
     */
    public fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = stored(base)
        if (tag.isEmpty()) return base
        val configuration = Configuration(base.resources.configuration)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }

    private fun stored(context: Context): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)
            .applicationLocales
            .takeIf { !it.isEmpty }
            ?.get(0)
            ?.language
            .orEmpty()
    } else {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
    }
}
