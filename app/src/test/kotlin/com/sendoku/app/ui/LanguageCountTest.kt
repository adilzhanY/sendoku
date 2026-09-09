package com.sendoku.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The number of languages, as the app has it and as the documents claim it.
 *
 * This is the number that rots fastest. It appears in a README table, in the store
 * description and in the note under that description explaining where every number in the
 * listing comes from, and none of those three notices when a language is added. A wrong
 * count in a store listing is a false claim about a feature, which is a different kind of
 * mistake from a stale README.
 *
 * The screen that asks a new player which language they read had exactly this bug and was
 * fixed the same way: it counts the enum rather than spelling the number out, which is why
 * there is nothing to check inside the app itself.
 */
class LanguageCountTest {

    /** Every language the app is written in, which is the enum without the phone-following row. */
    private val written: Int by lazy {
        val source = File("src/main/kotlin/com/sendoku/app/ui/Languages.kt")
        assertTrue("Languages.kt is missing", source.isFile)
        val body = source.readText().substringAfter("public enum class Language(").substringBefore("\n}")
        Regex("""\n\s{4}[A-Z_]+\("([^"]*)",""").findAll(body).count { it.groupValues[1].isNotEmpty() }
    }

    private fun repo(name: String): String {
        val file = File("../$name")
        assertTrue("$name is missing", file.isFile)
        return file.readText()
    }

    @Test
    fun `the guard is counting a real enum`() {
        assertTrue("only $written languages were parsed out of the enum", written >= 13)
    }

    @Test
    fun `the readme says how many languages there are`() {
        val row = repo("README.md").lineSequence().firstOrNull { it.startsWith("| **Languages**") }
        assertTrue("the README has no Languages row any more", row != null)
        val claimed = Regex("""\d+""").find(row!!)?.value?.toInt()
        assertEquals("the README claims a different number of languages", written, claimed)
    }

    @Test
    fun `the store listing names every language it claims`() {
        // The description lists them by name rather than by number, so the count and the names
        // have to agree with each other as well as with the app. A language added to the enum
        // and forgotten here is one the listing does not offer to the people who read it.
        // Anchored on the shape of the sentence rather than on the number spelled out in it,
        // because the number changes every time a language is added and a test that hardcodes
        // it fails for the one reason that is never interesting.
        val play = repo("PLAY.md")
        val sentence = Regex("""[A-Z][a-z]+ languages: ([^.]+)\.""").find(play)?.groupValues?.get(1)
        assertTrue("the store description no longer lists the languages", sentence != null)
        val named = sentence!!.split(",", " and ").count { it.isNotBlank() }
        assertEquals("the store description names a different number of languages", written, named)
    }

    @Test
    fun `the note explaining the listing numbers agrees too`() {
        // The listing carries a note saying where each of its numbers comes from, so that a
        // reviewer can check them. A note that is itself out of date is worse than no note.
        val play = repo("PLAY.md")
        assertTrue(
            "the note under the store description no longer says how many languages there are",
            Regex("""[A-Z][a-z]+ languages: `Language`""").containsMatchIn(play),
        )
    }
}
