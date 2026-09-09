package com.sendoku.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every plurals block has the categories its language actually has.
 *
 * Android picks an item by CLDR category, and falls back to `other` when the category it
 * wants is absent. That fallback is silent and it is usually wrong, because `other` was
 * written for a different set of numbers. Arabic shipped two blocks with only `one` and
 * `other`, the two English has, so counts of 2, of 3 to 10 and of 11 to 99 all rendered the
 * form written for 100 and up. Russian shipped the same two blocks with the same fault, and
 * its `other` held the genitive plural, so "2 clues" came out as the form for "5 clues".
 *
 * Lint does not catch this. Nor does anything comparing one language to the English, because
 * having fewer categories than English is impossible and having more is the normal case, so
 * the only correct comparison is against CLDR.
 *
 * Categories below are from CLDR's plurals.xml. Only cardinal categories that this app can
 * actually reach are required: every count it formats is a whole number, so a language whose
 * extra category exists only for decimals is not asked for it.
 */
class PluralCategoriesTest {

    /** What CLDR says each language needs, keyed by resource folder. */
    private val required = mapOf(
        "values" to setOf("one", "other"),
        "values-de" to setOf("one", "other"),
        "values-tr" to setOf("one", "other"),
        "values-es" to setOf("one", "other"),
        "values-it" to setOf("one", "other"),
        "values-fr" to setOf("one", "other"),
        "values-pt" to setOf("one", "other"),
        "values-hi" to setOf("one", "other"),
        "values-bn" to setOf("one", "other"),
        "values-da" to setOf("one", "other"),
        // Slavic: one for 1, 21, 31; few for 2 to 4; many for 0 and 5 to 20.
        "values-ru" to setOf("one", "few", "many", "other"),
        "values-uk" to setOf("one", "few", "many", "other"),
        // Arabic has the richest set of any language the app ships.
        "values-ar" to setOf("zero", "one", "two", "few", "many", "other"),
        // No grammatical number at all, so one form serves every count.
        "values-ja" to setOf("other"),
        "values-ko" to setOf("other"),
        "values-b+zh+Hans" to setOf("other"),
        "values-in" to setOf("other"),
        "values-vi" to setOf("other"),
        "values-th" to setOf("other"),
    )

    private fun blocks(folder: File): Map<String, Set<String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(folder, "strings.xml"))
        val plurals = document.getElementsByTagName("plurals")
        return (0 until plurals.length).associate { i ->
            val node = plurals.item(i)
            val items = node.childNodes
            val categories = (0 until items.length)
                .map { items.item(it) }
                .filter { it.nodeName == "item" }
                .map { it.attributes.getNamedItem("quantity").nodeValue }
                .toSet()
            node.attributes.getNamedItem("name").nodeValue to categories
        }
    }

    @Test
    fun `the guard knows about every language that ships`() {
        val shipped = File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values") && File(it, "strings.xml").isFile }
            .map { it.name }
            .toSet()
        assertEquals("a language ships that this guard has no CLDR rule for", shipped, required.keys)
    }

    @Test
    fun `every plurals block carries the categories its language needs`() {
        val missing = mutableListOf<String>()
        for ((folder, categories) in required) {
            for ((name, present) in blocks(File("src/main/res/$folder"))) {
                val absent = categories - present
                if (absent.isNotEmpty()) {
                    missing += "$folder $name is missing ${absent.sorted().joinToString()}, " +
                        "so those counts silently fall back to other"
                }
            }
        }
        assertTrue(
            "these render the wrong form for some counts:\n" + missing.joinToString("\n"),
            missing.isEmpty(),
        )
    }

    @Test
    fun `no plurals block carries a category its language does not have`() {
        // The other direction. A stray category is dead weight rather than a bug, but it is
        // also a sign somebody copied a block from a language with different rules, and the
        // next person to edit it will not know which items are live.
        val stray = mutableListOf<String>()
        for ((folder, categories) in required) {
            for ((name, present) in blocks(File("src/main/res/$folder"))) {
                val extra = present - categories
                if (extra.isNotEmpty()) {
                    stray += "$folder $name has ${extra.sorted().joinToString()}, which the language never asks for"
                }
            }
        }
        assertTrue("dead plural forms:\n" + stray.joinToString("\n"), stray.isEmpty())
    }

    @Test
    fun `every language declares the same plurals as the English`() {
        // A missing block is worse than a missing category: pluralStringResource has nothing
        // to read and the app falls back to the English wholesale.
        val english = blocks(File("src/main/res/values")).keys
        assertTrue("no plurals were found in the English at all", english.size >= 13)
        for (folder in required.keys - "values") {
            assertEquals(
                "$folder does not declare the same plurals",
                english,
                blocks(File("src/main/res/$folder")).keys,
            )
        }
    }
}
