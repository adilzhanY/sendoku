package com.sendoku.app.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File

/**
 * The four bundled faces, opened and asked what they can draw.
 *
 * Nothing about a font is visible in a diff. A subset built from the wrong character list,
 * or from an older set of strings, produces a file of entirely plausible size in which
 * every Turkish dotted i is an empty box. On a board that is the app broken, and the first
 * person to notice would be a player reading a language nobody here reads.
 *
 * So every file is opened and asked, character by character, whether it can draw the whole
 * of what the app's own strings contain. That is the same list tools/subset-fonts.py cuts
 * them down to, derived the same way, so adding a language and forgetting to rebuild the
 * fonts fails here rather than on somebody's phone.
 */
class BundledFontsTest {

    private val families = mapOf(
        "Inter" to listOf("inter_regular.ttf", "inter_semibold.ttf"),
        "PT Serif" to listOf("pt_serif_regular.ttf", "pt_serif_bold.ttf"),
        "Manrope" to listOf("manrope_regular.ttf", "manrope_semibold.ttf"),
        "JetBrains Mono" to listOf("jetbrains_mono_regular.ttf", "jetbrains_mono_bold.ttf"),
    )

    /** Every character the shipped strings can put on screen, plus what the board adds. */
    private val charset: Set<Char> by lazy {
        val found = HashSet<Char>()
        found += "0123456789:/%".toSet()
        File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values") }
            .mapNotNull { File(it, "strings.xml").takeIf(File::isFile) }
            .forEach { file ->
                Regex(">([^<>]+)<").findAll(file.readText()).forEach { match ->
                    found += match.groupValues[1].toSet()
                }
            }
        found.filter { !it.isWhitespace() && it.code >= 0x20 }.toSet()
    }

    private fun open(name: String): Font {
        val file = File("src/main/res/font/$name")
        assertTrue("$name is missing from res/font", file.isFile)
        assertTrue("$name is empty", file.length() > 1000)
        return Font.createFont(Font.TRUETYPE_FONT, file)
    }

    @Test
    fun `the character set is the one the app actually has`() {
        // A guard on the guard: if this collapses to ASCII, every check below passes while
        // testing nothing.
        assertTrue("only ${charset.size} characters were found", charset.size > 150)
        assertTrue("no Cyrillic was found", charset.any { it.code in 0x400..0x4FF })
        for (c in "äöüßğşıİçÇ") {
            assertTrue("the German and Turkish characters are missing from the set", c in charset)
        }
    }

    @Test
    fun `every face can draw every character in every language`() {
        for ((family, files) in families) {
            for (name in files) {
                val font = open(name)
                val missing = charset.filterNot(font::canDisplay).sorted()
                assertTrue(
                    "$family cannot draw ${missing.joinToString("")} in $name. Rerun tools/subset-fonts.py",
                    missing.isEmpty(),
                )
            }
        }
    }

    @Test
    fun `every face is still a subset rather than the whole family`() {
        // Whole, these four are 941 KB. The entire case for a face per theme is that they
        // are cut down, so a file that has quietly become complete has to fail here.
        for ((family, files) in families) {
            for (name in files) {
                val size = File("src/main/res/font/$name").length()
                assertTrue("$family: $name is $size bytes, which is too big to be a subset", size < 60_000)
            }
        }
    }

    @Test
    fun `every theme sets every style in its own face`() {
        // A theme whose grid is one family and whose buttons are another is not a theme.
        for (id in SendokuThemeId.entries) {
            val type = SendokuThemes.type(id)
            val styles = mapOf(
                "display" to type.display, "title" to type.title, "body" to type.body,
                "label" to type.label, "overline" to type.overline, "toolLabel" to type.toolLabel,
                "statLabel" to type.statLabel, "statValue" to type.statValue,
                "gridGiven" to type.gridGiven, "gridEntry" to type.gridEntry,
                "pencilMark" to type.pencilMark, "padDigit" to type.padDigit,
                "padCount" to type.padCount, "timer" to type.timer,
            )
            val family = styles.getValue("body").fontFamily
            assertTrue("$id leaves its body text on the platform font", family != null)
            for ((name, style) in styles) {
                assertEquals("$id draws $name in a different face from the rest of itself", family, style.fontFamily)
            }
        }
    }

    @Test
    fun `the four themes do not all wear the same face`() {
        val faces = SendokuThemeId.entries.map { SendokuThemes.type(it).body.fontFamily }.toSet()
        assertEquals("the point of this was a face per theme", SendokuThemeId.entries.size, faces.size)
    }
}
