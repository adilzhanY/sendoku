package com.sendoku.app.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the bundled font is a real font that really has the digits in it.
 *
 * The file is a server side subset fetched from Google Fonts, and it holds ten digits and a
 * colon and nothing else. Nothing about that is visible in the repository: a truncated
 * download, or a subset built from the wrong character list, produces a file of plausible
 * size that renders every digit as an empty box. On a sudoku board that is the entire app
 * broken, and it would first be noticed by a player.
 *
 * So the file is opened and asked, glyph by glyph, whether it can draw what the grid needs.
 */
class DigitFontTest {

    private val weights = listOf(
        "inter_digits_regular.ttf",
        "inter_digits_semibold.ttf",
    )

    private fun fontFile(name: String): File {
        // Unit tests run with the module directory as the working directory.
        val file = File("src/main/res/font/$name")
        assertTrue("$name is missing from res/font", file.isFile)
        return file
    }

    @Test
    fun `both weights are present and are valid truetype`() {
        for (name in weights) {
            val file = fontFile(name)
            assertTrue("$name is empty", file.length() > 1000)
            val font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file)
            // The semibold file reports itself as "Inter SemiBold", so match the family only.
            assertTrue("$name says it is ${font.family}", font.family.startsWith("Inter"))
        }
    }

    @Test
    fun `every digit the grid draws is in the font`() {
        for (name in weights) {
            val font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile(name))
            for (digit in '0'..'9') {
                assertTrue("$name cannot draw $digit", font.canDisplay(digit))
            }
            assertTrue("$name cannot draw the colon the timer needs", font.canDisplay(':'))
        }
    }

    @Test
    fun `the subset really is a subset, and has not quietly become the whole family`() {
        // A full weight of Inter is around three hundred kilobytes. Letting one in by
        // accident would be a quarter of the download for glyphs the app never draws.
        for (name in weights) {
            val size = fontFile(name).length()
            assertTrue("$name is $size bytes, which is too big to be digits only", size < 40_000)
        }
    }

    @Test
    fun `the font is only ever asked for digits`() {
        // It has no letters, so anything else would render as a box. This is the guard on
        // that rule: every style using the digit family must be a digit style.
        val digitStyles = mapOf(
            "gridGiven" to DefaultType.gridGiven,
            "gridEntry" to DefaultType.gridEntry,
            "pencilMark" to DefaultType.pencilMark,
            "padDigit" to DefaultType.padDigit,
            "padCount" to DefaultType.padCount,
            "timer" to DefaultType.timer,
        )
        for ((name, style) in digitStyles) {
            assertEquals("$name should use the bundled digits", DigitFont, style.fontFamily)
        }

        val proseStyles = mapOf(
            "display" to DefaultType.display,
            "title" to DefaultType.title,
            "body" to DefaultType.body,
            "label" to DefaultType.label,
            "overline" to DefaultType.overline,
        )
        for ((name, style) in proseStyles) {
            assertEquals("$name must stay on the platform font", null, style.fontFamily)
        }
    }
}
