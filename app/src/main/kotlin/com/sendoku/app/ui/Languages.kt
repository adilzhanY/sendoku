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
import java.text.Collator
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
    UKRAINIAN("uk", R.string.language_ukrainian),

    /**
     * Indonesian, written id here and in as a resource folder.
     *
     * Indonesian changed its ISO code from in to id in 1989 and Java never followed. The tag
     * is what goes out to the system, and BCP 47 wants id. What comes back from a Locale on
     * Android is in, because that is the code Java kept. So the two are not the same string
     * and comparing them directly says this language is not chosen when it is.
     */
    INDONESIAN("id", R.string.language_indonesian),
    VIETNAMESE("vi", R.string.language_vietnamese),
    HINDI("hi", R.string.language_hindi),

    /**
     * Bengali, with the numbering system named in the tag for the same reason Arabic has one.
     *
     * CLDR gives Bengali its own digits, ০ ১ ২ rather than 0 1 2. The board can only draw
     * Western ones, so a timer counting in one system next to a grid drawn in the other is
     * the first thing a Bengali player would notice. Resources still resolve on the language,
     * so this reads values-bn exactly as bn would.
     */
    BENGALI("bn-u-nu-latn", R.string.language_bengali),
    THAI("th", R.string.language_thai),
    DANISH("da", R.string.language_danish),
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

    /**
     * The order the picker lists languages in.
     *
     * Not the order they were added in, which is what this was until it reached eighteen and
     * became a list somebody has to look through rather than glance at. The rule is the one
     * Android's own per app language screen uses, and it is worth matching exactly, because
     * this app is registered in that screen: a player who opens it there and then opens the
     * picker here should not be handed the same eighteen languages in two different orders.
     *
     * AOSP sorts on the label, which for a language is its endonym, its name in its own
     * language, and it compares them through a Collator rather than by code point. Two things
     * follow from that and neither is obvious. Scripts group together, because collation
     * orders whole scripts before it orders letters, so the Latin names come first and the
     * Cyrillic, Arabic, Indic, Thai and CJK ones follow in blocks. And the comparison is done
     * in the reader's own locale, so a Thai reader sees Thai near the top where Thai collation
     * puts it, rather than buried behind five scripts they cannot read.
     *
     * The one special case is Arabic. Its endonym is العربية, which begins with the definite
     * article, so a plain sort files every Arabic list under A for al. AOSP strips it before
     * comparing and so does this.
     *
     * What is deliberately not copied is AOSP's suggested block, which floats the current and
     * the system language to the top. That exists because the system picker offers hundreds of
     * locales. Eighteen fit on two screens, the current one already carries a filled radio
     * button, and a list that rearranges itself every time the language changes is worse to
     * come back to than one that does not move.
     *
     * Following the phone is pinned first regardless. It is not a language, it is the default,
     * and it is the answer for most people.
     *
     * @param labels each language as it is written on screen, which is what is being sorted.
     * @param sortIn the reader's locale, whose collation rules decide the order.
     */
    public fun inDisplayOrder(labels: Map<Language, String>, sortIn: Locale): List<Language> {
        val collator = Collator.getInstance(sortIn)
        val rest = labels.keys.filter { it != Language.SYSTEM }
            .sortedWith(compareBy(collator) { forSorting(it, labels.getValue(it)) })
        return if (Language.SYSTEM in labels) listOf(Language.SYSTEM) + rest else rest
    }

    /** The Arabic definite article, which a sort has to look past rather than at. */
    private const val ARABIC_ARTICLE = "\u0627\u0644"

    private fun forSorting(language: Language, label: String): String =
        if (language == Language.ARABIC && label.startsWith(ARABIC_ARTICLE)) {
            label.removePrefix(ARABIC_ARTICLE)
        } else {
            label
        }

    /** What the app is set to, or [Language.SYSTEM] when it is following the phone. */
    public fun current(context: Context): Language {
        val tag = stored(context)
        if (tag.isEmpty()) return Language.SYSTEM
        // Compared on the language alone, because that is all a stored locale carries back.
        // A tag with a script in it, like zh-Hans, still comes home as zh.
        return Language.entries.firstOrNull { it.tag.isNotEmpty() && sameLanguage(it.tag, tag) } ?: Language.SYSTEM
    }

    /**
     * Whether two language codes name the same language.
     *
     * Not a string comparison, because three languages renamed themselves and Java kept the
     * old code for compatibility: Indonesian id is in, Hebrew he is iw, Yiddish yi is ji.
     * Android still does this, so a Locale for Indonesian reports in while every modern list,
     * including the tag on the enum and the one handed to the system, says id. Putting both
     * sides through a Locale settles it rather than listing the pairs here, and it keeps
     * working if a fourth language is ever added to that list.
     */
    internal fun sameLanguage(one: String, other: String): Boolean =
        Locale.forLanguageTag(one).language == Locale.forLanguageTag(other).language

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
