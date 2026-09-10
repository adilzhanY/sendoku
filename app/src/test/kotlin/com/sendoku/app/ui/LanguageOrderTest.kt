package com.sendoku.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Collator
import java.util.Locale

/**
 * The order the picker lists eighteen languages in.
 *
 * It used to be the order they were added in, which is not an order, it is a history. The rule
 * now is the one Android's own per app language screen uses, and the point of testing it here
 * rather than only looking at the screen is that the interesting cases are the ones a glance
 * at an English phone does not show: Arabic filed under its definite article, and a Thai
 * reader's list collated by Thai rules rather than English ones.
 *
 * The labels are the real ones, written out here rather than read from resources, because a
 * JVM test cannot resolve a string resource and because these are the strings being sorted.
 * They are checked against the resources by [LanguageLabelsTest].
 */
class LanguageOrderTest {

    private val labels: Map<Language, String> = mapOf(
        Language.SYSTEM to "Follow the phone",
        Language.ENGLISH to "English",
        Language.RUSSIAN to "Русский",
        Language.GERMAN to "Deutsch",
        Language.TURKISH to "Türkçe",
        Language.SPANISH to "Español",
        Language.ITALIAN to "Italiano",
        Language.JAPANESE to "日本語",
        Language.FRENCH to "Français",
        Language.PORTUGUESE to "Português (Brasil)",
        Language.CHINESE to "简体中文",
        Language.KOREAN to "한국어",
        Language.ARABIC to "العربية",
        Language.UKRAINIAN to "Українська",
        Language.INDONESIAN to "Bahasa Indonesia",
        Language.VIETNAMESE to "Tiếng Việt",
        Language.HINDI to "हिन्दी",
        Language.BENGALI to "বাংলা",
        Language.THAI to "ไทย",
        Language.DANISH to "Dansk",
        Language.DUTCH to "Nederlands",
        Language.NORWEGIAN to "Norsk bokmål",
        Language.SWEDISH to "Svenska",
    )

    private fun order(tag: String = "en") = Languages.inDisplayOrder(labels, Locale.forLanguageTag(tag))

    @Test
    fun `the test knows about every language the app has`() {
        // A guard on the guard. If a language is added and this map is not, every check below
        // quietly stops covering it.
        assertEquals("a language was added without being added here", Language.entries.toSet(), labels.keys)
    }

    @Test
    fun `following the phone comes first and is not sorted with the rest`() {
        // It is not a language, it is the default, and it is the answer for most people. Under
        // a plain sort it would land between English and Français.
        assertEquals(Language.SYSTEM, order().first())
    }

    @Test
    fun `every language is listed exactly once`() {
        val listed = order()
        assertEquals("a language was dropped or duplicated", Language.entries.size, listed.size)
        assertEquals("a language was duplicated", listed.toSet().size, listed.size)
    }

    @Test
    fun `the latin names are in alphabetical order`() {
        // Following the phone is left out: it is pinned rather than sorted, and it is only
        // spelled in Latin because this test reads the English file.
        val latin = order().drop(1).map { labels.getValue(it) }.filter { it.first().code < 0x0300 }
        assertTrue("no Latin names were found to check", latin.size >= 9)
        assertEquals("the Latin names are not in order", latin.sortedWith(Collator.getInstance()), latin)
        // Spelled out, because this is the part of the list most people actually read.
        assertEquals(
            listOf(
                "Bahasa Indonesia", "Dansk", "Deutsch", "English", "Español",
                "Français", "Italiano", "Nederlands", "Norsk bokmål", "Português (Brasil)",
                "Svenska", "Tiếng Việt", "Türkçe",
            ),
            latin,
        )
    }

    @Test
    fun `arabic is not filed under its definite article`() {
        // العربية begins with ال, the word for the. Sorted as it is written it lands among the
        // A names, which is where AOSP would have put it too had it not stripped the article.
        val listed = order()
        val arabic = listed.indexOf(Language.ARABIC)
        assertTrue(
            "Arabic sorted under its article, ahead of the Cyrillic names",
            arabic > listed.indexOf(Language.UKRAINIAN),
        )
    }

    @Test
    fun `each script is a block rather than scattered through the list`() {
        // Collation orders whole scripts before it orders letters, which is what makes a list
        // in six alphabets readable at all. If this ever fails, the sort has stopped being a
        // collation and become a comparison of code points or of something else entirely.
        val scripts = order().drop(1).map { Character.UnicodeScript.of(labels.getValue(it).first().code) }
        assertEquals("a script is split across the list", scripts.distinct().size, scripts.toRunLengths().size)
        assertTrue("hardly any scripts were found", scripts.distinct().size >= 5)
    }

    @Test
    fun `a thai reader gets thai where thai collation puts it`() {
        // The comparison happens in the reader's own locale, so this is not the same list in
        // every language. In Thai, Thai sorts up beside the Latin names instead of behind five
        // scripts a Thai reader cannot read. That is the whole reason for using a Collator.
        val english = order("en")
        val thai = order("th")
        assertTrue(
            "Thai is no higher for a Thai reader than for an English one, so the sort locale is ignored",
            thai.indexOf(Language.THAI) < english.indexOf(Language.THAI),
        )
        assertEquals("the two lists are not the same languages", english.toSet(), thai.toSet())
    }

    @Test
    fun `the order does not depend on the order the enum happens to declare`() {
        // Shuffling the input must not move anything, or the picker is still showing history.
        val shuffled = labels.entries.shuffled().associate { it.key to it.value }
        assertEquals(order(), Languages.inDisplayOrder(shuffled, Locale.ENGLISH))
    }

    private fun <T> List<T>.toRunLengths(): List<T> = fold(mutableListOf()) { runs, item ->
        if (runs.lastOrNull() != item) runs.add(item)
        runs
    }
}
