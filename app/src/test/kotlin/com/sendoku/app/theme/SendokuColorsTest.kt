package com.sendoku.app.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every colour that carries text is checked against WCAG AA.
 *
 * Not a formality. The first draft of these tokens had pencil marks at 3.5 to 1, which
 * looks fine to someone with good eyes on a good screen and is unreadable on a bus in
 * daylight. Nothing catches that by eye, so it is arithmetic or it is luck.
 */
class SendokuColorsTest {

    /** Smallest ratio WCAG AA accepts for text below eighteen point. */
    private val aa = 4.5

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    /** Lays a partly transparent colour over an opaque one, the way the screen will. */
    private fun over(top: Color, bottom: Color): Color {
        val a = top.alpha
        return Color(
            red = top.red * a + bottom.red * (1 - a),
            green = top.green * a + bottom.green * (1 - a),
            blue = top.blue * a + bottom.blue * (1 - a),
        )
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val front = luminance(over(foreground, background))
        val back = luminance(background)
        return (max(front, back) + 0.05) / (min(front, back) + 0.05)
    }

    private val themes = listOf("Deep Field dark" to DeepFieldDark, "Deep Field light" to DeepFieldLight)

    @Test
    fun `every colour that carries text passes AA on the board`() {
        for ((name, colors) in themes) {
            val text = mapOf(
                "given" to colors.given,
                "entry" to colors.entry,
                "pencil" to colors.pencil,
                "muted" to colors.muted,
                "conflict" to colors.conflict,
            )
            for ((label, color) in text) {
                val ratio = contrast(color, colors.surface)
                assertTrue("$name $label is $ratio to 1 on the surface", ratio >= aa)
            }
        }
    }

    @Test
    fun `every colour that carries text passes AA on the background too`() {
        for ((name, colors) in themes) {
            for ((label, color) in mapOf("given" to colors.given, "muted" to colors.muted)) {
                val ratio = contrast(color, colors.background)
                assertTrue("$name $label is $ratio to 1 on the background", ratio >= aa)
            }
        }
    }

    @Test
    fun `text on the accent is readable`() {
        for ((name, colors) in themes) {
            val ratio = contrast(colors.onAccent, colors.accent)
            assertTrue("$name onAccent is $ratio to 1", ratio >= aa)
        }
    }

    @Test
    fun `a digit stays readable through every wash that can sit under it`() {
        // Selection, peer and hint washes all paint behind a digit, so the digit has to
        // survive all of them and not just the bare surface.
        for ((name, colors) in themes) {
            val washes = mapOf(
                "selection" to colors.selection,
                "peer" to colors.peer,
                "match" to colors.match,
                "hintLogic" to colors.hintLogic,
                "conflictWash" to colors.conflictWash,
            )
            for ((label, wash) in washes) {
                val under = over(wash, colors.surface)
                for ((digit, color) in mapOf("given" to colors.given, "entry" to colors.entry)) {
                    val ratio = contrast(color, under)
                    assertTrue("$name $digit over $label is $ratio to 1", ratio >= aa)
                }
            }
        }
    }

    @Test
    fun `the dark theme is properly black`() {
        assertEquals(Color(0xFF000000), DeepFieldDark.background)
        assertTrue(DeepFieldDark.isDark)
        assertTrue(!DeepFieldLight.isDark)
    }

    @Test
    fun `there is one accent and the player's own digits wear it`() {
        for ((name, colors) in themes) {
            assertEquals("$name uses a different colour for entries than for the accent", colors.accent, colors.entry)
        }
    }

    @Test
    fun `a given never looks like an entry`() {
        // The single most important distinction on the board: what you may change.
        for ((name, colors) in themes) {
            // Colour is the secondary cue here. The weight of the type is the primary one,
            // which is checked in ColourBlindnessTest, because in some themes the two colours
            // land at nearly the same brightness and no palette can fix that.
            assertNotEquals(name, colors.given, colors.entry)
        }
    }

    @Test
    fun `a conflict cannot be mistaken for anything else`() {
        // Luminance contrast is a poor judge of two coloured foregrounds: a crimson and a
        // teal of the same brightness score 1.0 to 1 and are still obvious to most people.
        // What it does measure is whether they survive being seen without colour at all, and
        // some separation there is worth having. Colour is never the only cue for a mistake,
        // which is a matter for the accessibility work rather than for the palette.
        for ((name, colors) in themes) {
            assertNotEquals(name, colors.conflict, colors.accent)
            assertNotEquals(name, colors.conflict, colors.given)
            assertTrue(
                "$name conflict and entry are the same brightness, so a greyscale viewer sees one colour",
                contrast(colors.conflict, colors.entry) > 1.4,
            )
        }
    }

    @Test
    fun `the box line is heavier than the hairline`() {
        for ((name, colors) in themes) {
            val hairline = contrast(colors.hairline, colors.surface)
            val boxLine = contrast(colors.boxLine, colors.surface)
            assertTrue("$name draws box borders no stronger than cell borders", boxLine > hairline)
        }
    }

    @Test
    fun `both themes define the same set of colours`() {
        // The data class enforces this at compile time, so this only guards against someone
        // reaching for a default value to fill a gap.
        assertNotEquals(DeepFieldDark, DeepFieldLight)
        assertEquals(DeepFieldDark::class, DeepFieldLight::class)
    }
}
