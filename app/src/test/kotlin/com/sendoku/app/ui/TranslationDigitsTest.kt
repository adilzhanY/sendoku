package com.sendoku.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The digits in the prose have to be the digits on the board.
 *
 * The board draws 0 to 9 and nothing else, because those ten glyphs are what the four
 * bundled faces are cut down to. Four of the eighteen languages would otherwise be numbered
 * in their own digits, and the app already pins the numbering system in the locale tag to
 * stop that: Arabic is ar-u-nu-latn and Bengali is bn-u-nu-latn for exactly this reason.
 *
 * That fixes the numbers the app formats. It does nothing about a number a translator typed
 * into a sentence by hand, and a lesson that says eight of the nine digits in Bengali
 * numerals beside a grid drawn in Western ones is the app contradicting itself in the one
 * place it is trying to teach. Nothing else notices: the string is present, not empty, keeps
 * its format arguments and is plainly not English.
 */
class TranslationDigitsTest {

    /** Every script whose own digits could turn up in a sentence, and what they look like. */
    private val nativeDigits = mapOf(
        "Arabic-Indic" to '٠'..'٩',
        "extended Arabic-Indic" to '۰'..'۹',
        "Devanagari" to '०'..'९',
        "Bengali" to '০'..'৯',
        "Thai" to '๐'..'๙',
        "fullwidth" to '０'..'９',
    )

    private val folders: List<File> by lazy {
        val all = File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values") && File(it, "strings.xml").isFile }
        assertTrue("no strings files were found at all", all.size >= 18)
        all
    }

    /**
     * Every word each file actually ships, by name.
     *
     * Parsed rather than grepped, because the comments must not be searched. Two of these
     * files explain in a comment why their locale tag pins the numbering system, and they do
     * it by showing the digits they are rejecting. Grepping the file text caught that
     * explanation and called it the bug it exists to describe.
     */
    private fun shipped(folder: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(folder, "strings.xml"))
        val out = mutableMapOf<String, String>()
        val strings = document.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val node = strings.item(i)
            out[node.attributes.getNamedItem("name").nodeValue] = node.textContent
        }
        val plurals = document.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val node = plurals.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val items = node.childNodes
            for (j in 0 until items.length) {
                val item = items.item(j)
                if (item.nodeName != "item") continue
                val quantity = item.attributes.getNamedItem("quantity").nodeValue
                out["$name:$quantity"] = item.textContent
            }
        }
        return out
    }

    @Test
    fun `no language numbers its prose in its own digits`() {
        val found = mutableListOf<String>()
        for (folder in folders) {
            for ((name, value) in shipped(folder)) {
                for ((script, digits) in nativeDigits) {
                    val hits = value.filter { it in digits }.toSortedSet()
                    if (hits.isNotEmpty()) {
                        found += "${folder.name} $name has $script digits ${hits.joinToString("")}"
                    }
                }
            }
        }
        assertTrue(
            "the board can only draw 0 to 9, so these contradict the grid beside them:\n" +
                found.joinToString("\n"),
            found.isEmpty(),
        )
    }

    @Test
    fun `the guard would notice if a native digit appeared`() {
        // A guard on the guard. The check above passes on an empty ruleset just as happily as
        // on a clean one, so this proves the ranges match what they are meant to match.
        for ((script, digits) in nativeDigits) {
            assertTrue("$script is not ten digits long", digits.count() == 10)
        }
        assertTrue("Bengali four is not in the Bengali range", '৪' in nativeDigits.getValue("Bengali"))
        assertTrue("Western four is caught by mistake", nativeDigits.values.none { '4' in it })
    }

    @Test
    fun `grid coordinates survive translation`() {
        // Three lessons point at named cells of a real board, r1c4 and r3c9 and r1c8. They are
        // coordinates rather than words, so a translation that localises or renumbers one
        // sends the reader to the wrong cell while the sentence still reads perfectly.
        val cell = Regex("""\br\d+c\d+\b""")
        val coordinates = shipped(File("src/main/res/values"))
            .values.flatMap { cell.findAll(it).map(MatchResult::value) }.toSortedSet()
        assertTrue("no coordinates were found in the English at all", coordinates.size >= 3)

        for (folder in folders) {
            if (folder.name == "values") continue
            val theirs = shipped(folder)
                .values.flatMap { cell.findAll(it).map(MatchResult::value) }.toSortedSet()
            assertTrue(
                "${folder.name} names cells $theirs where the English names $coordinates",
                theirs == coordinates,
            )
        }
    }
}
