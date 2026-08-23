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
 * Every theme is held to the same standard as the first one.
 *
 * Three more looks were the whole reason the tokens were built the way they were, and the
 * risk with a second theme is always that it is checked by eye and shipped. Contrast is
 * arithmetic, so it gets done by arithmetic, for all of them.
 */
class ThemeCatalogTest {

    private val aa = 4.5

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

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

    /** Every colour set the app can put on screen, however it is reached. */
    private val everyPalette: List<Pair<String, SendokuColors>> =
        SendokuThemeId.entries.flatMap { theme ->
            listOf(false, true).map { dark ->
                "${theme.displayName} ${if (dark) "dark" else "light"}" to
                    SendokuThemes.colors(theme, dark)
            }
        }.distinctBy { it.second }

    @Test
    fun `every theme has a colour set for light and for dark`() {
        for (theme in SendokuThemeId.entries) {
            SendokuThemes.colors(theme, dark = true)
            SendokuThemes.colors(theme, dark = false)
            SendokuThemes.type(theme)
            SendokuThemes.dimens(theme)
            SendokuThemes.motion(theme)
        }
    }

    @Test
    fun `every colour that carries text passes AA in every theme`() {
        for ((name, colors) in everyPalette) {
            val text = mapOf(
                "given" to colors.given,
                "entry" to colors.entry,
                "pencil" to colors.pencil,
                "muted" to colors.muted,
                "conflict" to colors.conflict,
            )
            for ((label, color) in text) {
                val ratio = contrast(color, colors.surface)
                assertTrue("$name $label is $ratio to 1", ratio >= aa)
            }
            assertTrue("$name onAccent", contrast(colors.onAccent, colors.accent) >= aa)
        }
    }

    @Test
    fun `a digit survives every wash in every theme`() {
        for ((name, colors) in everyPalette) {
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
                    assertTrue("$name $digit over $label", contrast(color, under) >= aa)
                }
            }
        }
    }

    @Test
    fun `a peer wash is visible in every theme`() {
        // The first version of Deep Field had one at four levels of RGB, which is the same as
        // not having one. Everything since is checked rather than eyeballed.
        for ((name, colors) in everyPalette) {
            val washed = over(colors.peer, colors.surface)
            val difference = kotlin.math.abs(luminance(washed) - luminance(colors.surface))
            assertTrue("$name peer wash is invisible", difference > 0.004)
        }
    }

    @Test
    fun `the selection is always louder than a peer`() {
        for ((name, colors) in everyPalette) {
            val peer = kotlin.math.abs(
                luminance(over(colors.peer, colors.surface)) - luminance(colors.surface),
            )
            val selection = kotlin.math.abs(
                luminance(over(colors.selection, colors.surface)) - luminance(colors.surface),
            )
            assertTrue("$name selection does not stand out from its peers", selection > peer)
        }
    }

    @Test
    fun `a given never looks like an entry, in any theme`() {
        for ((name, colors) in everyPalette) {
            // Colour is the secondary cue here. The weight of the type is the primary one,
            // which is checked in ColourBlindnessTest, because in some themes the two colours
            // land at nearly the same brightness and no palette can fix that.
            assertNotEquals(name, colors.given, colors.entry)
        }
    }

    @Test
    fun `the box line is always heavier than the hairline`() {
        for ((name, colors) in everyPalette) {
            val hairline = contrast(colors.hairline, colors.surface)
            val boxLine = contrast(colors.boxLine, colors.surface)
            assertTrue("$name box borders are no stronger than cell borders", boxLine > hairline)
        }
    }

    @Test
    fun `terminal ignores the light setting, and says so`() {
        assertTrue(SendokuThemes.isFixed(SendokuThemeId.TERMINAL))
        assertEquals(
            SendokuThemes.colors(SendokuThemeId.TERMINAL, dark = true),
            SendokuThemes.colors(SendokuThemeId.TERMINAL, dark = false),
        )
        for (theme in SendokuThemeId.entries - SendokuThemeId.TERMINAL) {
            assertTrue(!SendokuThemes.isFixed(theme))
            assertNotEquals(
                theme.displayName,
                SendokuThemes.colors(theme, dark = true),
                SendokuThemes.colors(theme, dark = false),
            )
        }
    }

    @Test
    fun `terminal has no rounding anywhere`() {
        val dimens = SendokuThemes.dimens(SendokuThemeId.TERMINAL)
        assertEquals(0f, dimens.radiusM.value)
        assertEquals(0f, dimens.boardRadius.value)
        assertEquals(0f, dimens.cellRadius.value)
    }

    @Test
    fun `every theme keeps its touch targets`() {
        for (theme in SendokuThemeId.entries) {
            assertTrue(SendokuThemes.dimens(theme).minTouchTarget.value >= 48f)
        }
    }

    @Test
    fun `every theme names itself and says what it is`() {
        for (theme in SendokuThemeId.entries) {
            assertTrue(theme.displayName.isNotBlank())
            assertTrue("${theme.name} has no summary", theme.summary.length > 15)
        }
        assertEquals(SendokuThemeId.entries.size, SendokuThemeId.entries.map { it.displayName }.toSet().size)
    }
}
