package com.sendoku.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The strings the app glues together at runtime, checked as a player would see them.
 *
 * A hint's region is built in three steps. `house_row` becomes "row 4", `hint_in_region`
 * wraps it as "in row 4", and `hint_look_here` wraps that again as "There is something to
 * find in row 4." Three resources, one sentence, and no single file shows the result.
 *
 * So a translator who puts the preposition in the wrapper AND in the inner string ships
 * "there is something to find in in row 4", and nothing notices: the string is present, not
 * empty, keeps its format argument and is plainly not English, so every other check in this
 * project passes it. Four of the eighteen languages had exactly this, in four scripts, and it
 * was found only by reading the Kotlin alongside the resources.
 *
 * This checks the half that is mechanically decidable, which is that the affix must not
 * appear twice. Whether a language needs one at all is a judgement: French once had it in
 * neither string, rendering "something to find of the row 3", and no test can know that.
 */
class ComposedStringsTest {

    private fun strings(folder: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(folder, "strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    private val folders: List<File> by lazy {
        val all = File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values") && File(it, "strings.xml").isFile }
        assertTrue("no strings files were found at all", all.size >= 18)
        all
    }

    /** The words a wrapper adds around its argument, with the argument itself taken out. */
    private fun added(pattern: String): List<String> = pattern
        .split(ARGUMENT)
        .flatMap { it.split(" ") }
        .map { it.trim(' ', '.', ',', ':', ';', '?', '!') }
        .filter { it.isNotEmpty() }

    /**
     * The words that sit immediately either side of the argument in a wrapper.
     *
     * Only the neighbours matter. Looking anywhere in the sentence reports a false alarm the
     * moment the same preposition is used for something else: Spanish's hint_cells_here says
     * "Las casillas en las que se apoya están iluminadas %1$s", where the "en" belongs to a
     * relative clause and the argument is not prefixed at all.
     */
    private fun neighbours(wrapper: String): List<String> = listOf(
        wrapper.substringBefore(ARGUMENT, "").trimEnd().substringAfterLast(' '),
        wrapper.substringAfter(ARGUMENT, "").trimStart().substringBefore(' '),
    ).map { it.trim(' ', '.', ',', ':', ';', '?', '!', ' ') }.filter { it.isNotEmpty() }

    /**
     * Whether one of those neighbours is the word the inner string already added.
     *
     * Scripts that space their words are compared whole, or German's "finden" matches "in"
     * and Ukrainian's "Тут" matches "у". Scripts that do not space their words have to be
     * compared at the edges, which is the whole reason Thai needed checking.
     */
    private fun doubles(word: String, neighbour: String): Boolean {
        val unspaced = Character.UnicodeScript.of(word.first().code) in setOf(
            Character.UnicodeScript.THAI,
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HANGUL,
            Character.UnicodeScript.KHMER,
            Character.UnicodeScript.LAO,
        )
        return if (unspaced) {
            neighbour.endsWith(word) || neighbour.startsWith(word)
        } else {
            neighbour == word
        }
    }

    @Test
    fun `no language names a region twice over`() {
        val broken = mutableListOf<String>()
        for (folder in folders) {
            val strings = strings(folder)
            val inner = strings["hint_in_region"] ?: continue
            // What hint_in_region contributes: "in", "у", "ใน", "में" and so on. Empty when the
            // language puts it in the wrapper instead, which is equally correct.
            val affix = added(inner)
            if (affix.isEmpty()) continue

            for (name in listOf("hint_look_here", "hint_cells_here")) {
                val wrapper = strings[name] ?: continue
                val beside = neighbours(wrapper)
                val twice = affix.filter { word -> beside.any { doubles(word, it) } }
                if (twice.isNotEmpty()) {
                    broken += "${folder.name} $name already says ${twice.joinToString()} " +
                        "next to the argument, which hint_in_region adds as well"
                }
            }
        }
        assertTrue(
            "these would render a doubled preposition on the hint panel:\n" +
                broken.joinToString("\n"),
            broken.isEmpty(),
        )
    }

    @Test
    fun `the guard is looking at strings that really do compose`() {
        // A guard on the guard. If these resources are renamed, or the hint panel stops
        // joining them, the check above silently tests nothing at all.
        val copy = File("src/main/kotlin/com/sendoku/app/ui/TechniqueCopy.kt").readText()
        for (name in listOf("hint_in_region", "hint_in_regions", "house_row")) {
            assertTrue("$name is no longer composed in TechniqueCopy", copy.contains(name))
        }
        val panel = File("src/main/kotlin/com/sendoku/app/ui/HintPanel.kt").readText()
        assertTrue("hint_look_here no longer takes a composed region", panel.contains("hint_look_here"))
    }

    @Test
    fun `the matcher tells a whole word from a fragment`() {
        // The two mistakes this matcher exists to avoid, pinned down so a later tidy cannot
        // reintroduce either of them.
        assertTrue("the English affix is not one word", added("in %1\$s") == listOf("in"))
        assertTrue("a bare argument should add nothing", added("%1\$s").isEmpty())

        // Only the words touching the argument are the neighbours.
        assertTrue("the word before was missed", "tìm" in neighbours("Có điều để tìm %1\$s."))
        assertTrue("the word after was missed", "में" in neighbours("%1\$s में कुछ"))
        assertTrue(
            "a word far from the argument was treated as a neighbour",
            "en" !in neighbours("Las casillas en las que se apoya están iluminadas %1\$s."),
        )

        // And the two ways of comparing them.
        assertTrue("a whole word was not matched", doubles("in", "in"))
        assertTrue("German finden was read as in", !doubles("in", "finden"))
        assertTrue("Ukrainian Tut was read as u", !doubles("у", "Тут"))
        assertTrue("Thai was not matched at the edge", doubles("ใน", "มีอะไรให้หาใน"))
    }

    private companion object {
        /** The format argument these wrappers take, spelled so it is not one itself. */
        const val ARGUMENT = "%1\$s"
    }
}
