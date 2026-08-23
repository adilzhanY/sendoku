package com.sendoku.app.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import com.sendoku.app.ui.decorationFor
import com.sendoku.app.ui.describe
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The board, seen by somebody who cannot separate the colours it uses.
 *
 * Roughly one man in twelve has some form of red green colour blindness, which is a lot of
 * people to hand a board where a mistake is marked in red and their own digits in green.
 * A simulator app would show this to an eye that can already tell them apart; the honest
 * check is to transform the colours and measure what is left.
 *
 * The matrices are the standard Brettel and Viénot approximations, which are what every
 * simulator is doing behind its filter.
 */
class ColourBlindnessTest {

    private enum class Vision(val label: String, val matrix: FloatArray) {
        /** No red cones. */
        PROTANOPIA(
            "protanopia",
            floatArrayOf(0.567f, 0.433f, 0f, 0.558f, 0.442f, 0f, 0f, 0.242f, 0.758f),
        ),

        /** No green cones. The most common of the three. */
        DEUTERANOPIA(
            "deuteranopia",
            floatArrayOf(0.625f, 0.375f, 0f, 0.7f, 0.3f, 0f, 0f, 0.3f, 0.7f),
        ),

        /** No blue cones. */
        TRITANOPIA(
            "tritanopia",
            floatArrayOf(0.95f, 0.05f, 0f, 0f, 0.433f, 0.567f, 0f, 0.475f, 0.525f),
        ),

        /** Everything as grey, which is also what a bad screen in sunlight does. */
        GREYSCALE("greyscale", floatArrayOf(0.299f, 0.587f, 0.114f, 0.299f, 0.587f, 0.114f, 0.299f, 0.587f, 0.114f)),
    }

    private fun Vision.simulate(color: Color): Color {
        val m = matrix
        return Color(
            red = (m[0] * color.red + m[1] * color.green + m[2] * color.blue).coerceIn(0f, 1f),
            green = (m[3] * color.red + m[4] * color.green + m[5] * color.blue).coerceIn(0f, 1f),
            blue = (m[6] * color.red + m[7] * color.green + m[8] * color.blue).coerceIn(0f, 1f),
            alpha = color.alpha,
        )
    }

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val first = luminance(a)
        val second = luminance(b)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    /** How far apart two colours are once hue has been taken away from the viewer. */
    private fun separation(vision: Vision, a: Color, b: Color): Double {
        val first = vision.simulate(a)
        val second = vision.simulate(b)
        val byLuminance = contrast(first, second)
        val byChannel = maxOf(
            abs(first.red - second.red),
            abs(first.green - second.green),
            abs(first.blue - second.blue),
        ).toDouble()
        return max(byLuminance - 1.0, byChannel)
    }

    private val palettes: List<Pair<String, SendokuColors>> =
        SendokuThemeId.entries.flatMap { theme ->
            listOf(false, true).map { dark ->
                "${theme.displayName} ${if (dark) "dark" else "light"}" to
                    SendokuThemes.colors(theme, dark)
            }
        }.distinctBy { it.second }

    @Test
    fun `text stays readable on the surface for every kind of colour blindness`() {
        for ((name, colors) in palettes) {
            for (vision in Vision.entries) {
                val surface = vision.simulate(colors.surface)
                for ((label, color) in mapOf(
                    "given" to colors.given,
                    "entry" to colors.entry,
                    "conflict" to colors.conflict,
                    "muted" to colors.muted,
                )) {
                    val ratio = contrast(vision.simulate(color), surface)
                    assertTrue(
                        "$name $label is $ratio to 1 under ${vision.label}",
                        ratio >= 4.5,
                    )
                }
            }
        }
    }

    @Test
    fun `a clue is told apart by weight, not by colour alone`() {
        // Measured, rather than hoped: in Ink under greyscale the clue and the entry are 0.13
        // apart, which is nothing. Colour cannot carry this distinction in every theme for
        // every viewer, so the type does. A clue is semibold and an entry is not.
        for (theme in SendokuThemeId.entries) {
            val type = SendokuThemes.type(theme)
            assertTrue(
                "${theme.displayName} draws clues and entries in the same weight",
                type.gridGiven.fontWeight != type.gridEntry.fontWeight,
            )
        }
        // Colour is still the secondary cue, so it must at least differ.
        for ((name, colors) in palettes) {
            assertTrue("$name uses one colour for clues and entries", colors.given != colors.entry)
        }
    }

    @Test
    fun `a mistake is told apart by the underline, not by colour alone`() {
        // Zen's conflict and entry are 0.03 apart in greyscale, which is invisible. The line
        // under the digit is what a player actually sees, and it is drawn whatever the
        // colours do.
        assertTrue(decorationFor(isConflict = true) == TextDecoration.Underline)
        assertTrue(decorationFor(isConflict = false) == null)

        for ((name, colors) in palettes) {
            assertTrue("$name uses one colour for mistakes and entries", colors.conflict != colors.entry)
        }
    }

    @Test
    fun `a screen reader is told about a repeat in words`() {
        // The other half of not relying on colour: somebody who cannot see the board at all
        // still hears which cells are wrong.
        val puzzle = com.sendoku.engine.catalog.GradedGenerator(
            com.sendoku.engine.Dimensions.CLASSIC,
            kotlin.random.Random(9601),
        ).let { maker ->
            var made: com.sendoku.engine.catalog.RatedPuzzle? = null
            while (made == null) made = maker.next()
            made
        }
        val state = com.sendoku.app.game.GameState.start(puzzle)
        val cell = state.cells.indices.first { state.cells[it].isGiven }
        assertTrue(describe(state, cell, conflicting = true).endsWith("repeated"))
        assertTrue(!describe(state, cell, conflicting = false).endsWith("repeated"))
    }

    @Test
    fun `the accent is still visible against the surface without any hue at all`() {
        for ((name, colors) in palettes) {
            val grey = Vision.GREYSCALE
            val ratio = contrast(grey.simulate(colors.accent), grey.simulate(colors.surface))
            assertTrue("$name accent vanishes in greyscale, $ratio to 1", ratio >= 3.0)
        }
    }
}
