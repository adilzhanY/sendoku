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

    /**
     * What no bundled face is asked to draw.
     *
     * Kept in step with NOT_OURS in tools/subset-fonts.py. None of the four has a kana, a
     * Chinese character, a Hangul syllable or an Arabic letter in it, and none of them is
     * going to: Android carries Noto for all of these and picks it up per character, so they
     * render on every phone without a byte from us. The one thing to hold onto is that this
     * list stays small and deliberate rather than growing into a hole the guard below can
     * fall through, which is why an accented Latin letter must never appear in it.
     */
    private val notOurs = listOf(
        0x0600..0x06FF, // Arabic
        0x0750..0x077F, // Arabic supplement
        0x08A0..0x08FF, // Arabic extended-A
        0x1100..0x11FF, // Hangul jamo
        0x3000..0x303F, // CJK punctuation
        0x3040..0x309F, // hiragana
        0x30A0..0x30FF, // katakana
        0x3130..0x318F, // Hangul compatibility jamo
        0x3400..0x4DBF, // CJK ideographs, extension A
        0x4E00..0x9FFF, // CJK ideographs
        0xAC00..0xD7AF, // Hangul syllables
        0xFB50..0xFDFF, // Arabic presentation forms-A
        0xFE70..0xFEFF, // Arabic presentation forms-B
        0xFF00..0xFFEF, // halfwidth and fullwidth forms
    )

    private fun Char.isNotOurs(): Boolean = notOurs.any { code in it }

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
        found.filter { !it.isWhitespace() && it.code >= 0x20 && !it.isNotOurs() }.toSet()
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
        for (c in "äöüßğşıİçÇñáíóúü¿àèìòù") {
            assertTrue("$c is missing from the set the faces are cut to", c in charset)
        }
        assertTrue("Japanese should be left to the phone rather than cut into the faces", '日' !in charset)
        assertTrue("Korean should be left to the phone too", '한' !in charset)
        assertTrue("Chinese should be left to the phone too", '数' !in charset)
    }

    @Test
    fun `japanese is left to the phone, deliberately and visibly`() {
        // 日本語 is the name of Japanese in the language picker, and it is written in Japanese
        // in every language file including the English one. So this is not hypothetical even
        // before a word of the app is translated: something has to draw it, and it is not
        // going to be a face cut down to a hundred and seventy Latin characters.
        val inStrings = File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values") }
            .mapNotNull { File(it, "strings.xml").takeIf(File::isFile) }
            .flatMap { file -> file.readText().toList() }
            .filter { it.isNotOurs() }
            .toSet()
        assertTrue("no Japanese was found in any language file", inStrings.isNotEmpty())
        for ((family, files) in families) {
            for (name in files) {
                val font = open(name)
                val drawn = inStrings.filter(font::canDisplay)
                assertTrue(
                    "$family draws ${drawn.joinToString("")} in $name, so the subset has grown a CJK range",
                    drawn.isEmpty(),
                )
            }
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
