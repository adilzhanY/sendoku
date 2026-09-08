package com.sendoku.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The names in the picker, read off disk.
 *
 * [LanguageOrderTest] sorts the real labels, which it can only do by holding a copy of them,
 * and a copy that has drifted from the strings file would let that test pass on words the app
 * does not show. This is what keeps the copy honest.
 *
 * It also pins down the thing that makes a language picker work at all: every name is written
 * in the language it names, so somebody who reads only Thai can find Thai. That is easy to get
 * right once and then quietly lose the next time a language is added by somebody translating
 * as they go.
 */
class LanguageLabelsTest {

    /** What values/strings.xml says each language is called. */
    private val english: Map<String, String> by lazy {
        val text = File("src/main/res/values/strings.xml").readText()
        Regex("""<string name="(language_[a-z]+)">([^<]*)</string>""")
            .findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun `every language in the enum has a name in the strings file`() {
        val source = File("src/main/kotlin/com/sendoku/app/ui/Languages.kt").readText()
        val named = Regex("""R\.string\.(language_[a-z]+)""").findAll(source).map { it.groupValues[1] }.toList()
        assertEquals("the enum and the strings file disagree", Language.entries.size, named.size)
        for (name in named) {
            assertTrue("$name is in the enum but not in values/strings.xml", name in english)
            assertTrue("$name is empty", english.getValue(name).isNotBlank())
        }
    }

    @Test
    fun `a language name is the same word in every language file`() {
        // A name is a proper noun written in its own language, so it is never translated. If
        // one file translates it, the picker stops being usable by the person it is for: the
        // whole point is that somebody who reads only Bengali can find বাংলা.
        val folders = File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values-") && File(it, "strings.xml").isFile }
        assertTrue("no translations were found", folders.size >= 17)
        for (folder in folders) {
            val text = File(folder, "strings.xml").readText()
            val theirs = Regex("""<string name="(language_[a-z]+)">([^<]*)</string>""")
                .findAll(text)
                .associate { it.groupValues[1] to it.groupValues[2] }
            for ((name, word) in theirs) {
                // Following the phone is a sentence rather than a name, so it is translated.
                if (name == "language_system") continue
                assertEquals("${folder.name} translated $name, which is a proper noun", english[name], word)
            }
        }
    }
}
