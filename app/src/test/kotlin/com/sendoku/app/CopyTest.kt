package com.sendoku.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The words, read straight off disk.
 *
 * Once the copy moved into resources it stopped being reachable from a plain unit test, and
 * the temptation is to stop checking it. That would be the wrong thing to give up: the
 * technique explanations are the teaching value of the whole app, and a placeholder or a
 * missing translation in there is worse than a crash, because nobody notices.
 *
 * So the files are parsed. That also means the translations get the same scrutiny as the
 * English, which is the part most likely to rot.
 */
class CopyTest {

    private fun strings(locale: String?): Map<String, String> {
        val directory = if (locale == null) "values" else "values-$locale"
        val file = File("src/main/res/$directory/strings.xml")
        assertTrue("${file.path} is missing", file.isFile)
        val text = file.readText()
        return Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
    }

    private val english by lazy { strings(null) }

    private val techniques = listOf(
        "naked_single", "hidden_single", "locked_candidates_pointing", "locked_candidates_claiming",
        "naked_pair", "hidden_pair", "naked_triple", "hidden_triple", "naked_quad", "hidden_quad",
        "x_wing", "simple_colouring", "xy_wing", "xyz_wing", "w_wing", "swordfish", "remote_pairs",
        "unique_rectangle", "bug_plus_one", "jellyfish", "multi_colouring", "x_chain", "xy_chain",
        "als_xz",
    )

    @Test
    fun `every technique has a name, a look for and an explanation`() {
        for (technique in techniques) {
            for (prefix in listOf("technique", "look", "because")) {
                assertTrue("$prefix\\_$technique is missing", english.containsKey("${prefix}_$technique"))
            }
        }
        assertEquals(24, techniques.size)
    }

    @Test
    fun `no explanation is a placeholder`() {
        for (technique in techniques) {
            val look = english.getValue("look_$technique")
            val because = english.getValue("because_$technique")
            assertTrue("look_$technique is too short", look.length > 30)
            assertTrue("because_$technique is too short", because.length > 60)
            assertTrue("look_$technique has no full stop", look.endsWith("."))
            assertTrue("because_$technique has no full stop", because.endsWith("."))
        }
    }

    @Test
    fun `no explanation leans on jargon the player has not been given`() {
        // The name of a technique is allowed to be jargon, because the hint teaches it. The
        // explanation is not, or the hint teaches nothing.
        val jargon = listOf("candidate elimination", "conjugate", "strong link", "bivalue", "als")
        for (technique in techniques) {
            val because = english.getValue("because_$technique").lowercase()
            for (phrase in jargon) {
                val found = Regex("\\b" + Regex.escape(phrase) + "\\b").containsMatchIn(because)
                assertTrue("because_$technique explains itself with '$phrase'", !found)
            }
        }
    }

    @Test
    fun `nothing is left in the code that should be in the strings file`() {
        // The lint rule catches this at build time. This catches it in a second, locally, and
        // says which file to look in.
        val offenders = File("src/main/kotlin").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex().mapNotNull { (number, line) ->
                    val trimmed = line.trim()
                    val looksLikeCopy = Regex("""(?:Text\(|label = |text = )"[A-Z][^"]{6,}"""")
                    if (looksLikeCopy.containsMatchIn(trimmed)) "${file.name}:${number + 1}  $trimmed" else null
                }
            }
            .toList()
        assertTrue("user facing text still in Kotlin:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    private fun plurals(locale: String?): Set<String> {
        val directory = if (locale == null) "values" else "values-$locale"
        val text = File("src/main/res/$directory/strings.xml").readText()
        return Regex("""<plurals name="([^"]+)"""").findAll(text).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `every translation covers every plural too`() {
        val locales = File("src/main/res").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").isFile }
            .map { it.name.removePrefix("values-") }
        for (locale in locales) {
            assertEquals("$locale is missing a plural", plurals(null), plurals(locale))
        }
    }

    @Test
    fun `every translation covers every string`() {
        val locales = File("src/main/res").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").isFile }
            .map { it.name.removePrefix("values-") }

        for (locale in locales) {
            val translated = strings(locale)
            val missing = english.keys - translated.keys
            assertTrue("$locale is missing ${missing.size} strings: ${missing.take(5)}", missing.isEmpty())
            val extra = translated.keys - english.keys
            assertTrue("$locale has strings English does not: ${extra.take(5)}", extra.isEmpty())
        }
    }

    @Test
    fun `every translation keeps the same format arguments`() {
        // A translation that drops a %1$s or swaps two positional arguments crashes at
        // runtime, in that language only, which is the hardest kind of bug to notice.
        val locales = File("src/main/res").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").isFile }
            .map { it.name.removePrefix("values-") }

        val argument = Regex("""%\d+\$[sd]""")
        for (locale in locales) {
            val translated = strings(locale)
            for ((key, value) in english) {
                val expected = argument.findAll(value).map { it.value }.toSortedSet()
                val actual = argument.findAll(translated[key] ?: "").map { it.value }.toSortedSet()
                assertEquals("$locale $key has the wrong format arguments", expected, actual)
            }
        }
    }
}
