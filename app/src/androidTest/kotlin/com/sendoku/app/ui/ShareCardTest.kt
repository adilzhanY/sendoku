package com.sendoku.app.ui

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.theme.SendokuThemeId
import com.sendoku.app.theme.SendokuThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The card that gets shared, drawn for real so it can be looked at. */
@RunWith(AndroidJUnit4::class)
class ShareCardTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The same bridge the app uses, outside a composition.
     *
     * Kept next to the real one rather than reaching into it, because a test that builds the
     * look differently from the app is a test of nothing. Both read the theme's own colours
     * and the theme's own font, and that is the part worth pinning.
     */
    private fun look(theme: SendokuThemeId, dark: Boolean = true): ShareCard.Look {
        val colors = SendokuThemes.colors(theme, dark)
        val (regular, bold) = SendokuThemes.fonts(theme)
        return ShareCard.Look(
            background = colors.background.toArgb(),
            board = colors.surface.toArgb(),
            hairline = colors.hairline.toArgb(),
            boxLine = colors.boxLine.toArgb(),
            given = colors.muted.toArgb(),
            entry = colors.given.toArgb(),
            muted = colors.muted.toArgb(),
            accent = colors.accent.toArgb(),
            warn = colors.conflict.toArgb(),
            regular = ResourcesCompat.getFont(context, regular) ?: Typeface.SANS_SERIF,
            bold = ResourcesCompat.getFont(context, bold) ?: Typeface.DEFAULT_BOLD,
        )
    }

    private fun card(
        title: String = "Solved",
        grade: String = "Diabolical",
        look: ShareCard.Look = look(SendokuThemeId.DEEP_FIELD),
    ) = ShareCard.draw(
        appName = "Sendoku",
        title = title,
        grade = grade,
        lines = listOf(
            ShareCard.Line("Time", "27:41"),
            ShareCard.Line("Mistakes", "1 of 3"),
            ShareCard.Line("Hints", "2 of 3"),
        ),
        // A real board, because the board is most of the picture now.
        grid = ShareCard.Grid(
            size = 9,
            boxWidth = 3,
            boxHeight = 3,
            digits = (0 until 81).map { (it % 9) + 1 },
            given = (0 until 81).filter { it % 3 == 0 }.toSet(),
        ),
        look = look,
    )

    @Test
    fun theCardIsDrawnAtTheSizeItClaims() {
        val bitmap = card()
        assertEquals(ShareCard.WIDTH, bitmap.width)
        assertEquals(ShareCard.HEIGHT, bitmap.height)
    }

    @Test
    fun theCardCarriesTheBoardItWasPlayedOn() {
        // The point of the picture. A card with the numbers but not the grid is a receipt.
        val withBoard = card()
        val withoutBoard = ShareCard.draw(
            appName = "Sendoku",
            title = "Solved",
            grade = "Diabolical",
            lines = listOf(ShareCard.Line("Time", "27:41")),
            grid = null,
            look = look(SendokuThemeId.DEEP_FIELD),
        )
        var different = 0
        for (x in 0 until ShareCard.WIDTH step 8) {
            for (y in 300 until 1100 step 8) {
                if (withBoard.getPixel(x, y) != withoutBoard.getPixel(x, y)) different++
            }
        }
        assertTrue("the board was not drawn on the card", different > 1000)
    }

    @Test
    fun theCardIsNotBlank() {
        // A canvas that threw halfway would still hand back a bitmap, and it would be one flat
        // colour. Counting distinct colours is the cheapest way to know something was drawn.
        val bitmap = card()
        val colours = buildSet {
            for (x in 0 until bitmap.width step 8) {
                for (y in 0 until bitmap.height step 8) add(bitmap.getPixel(x, y))
            }
        }
        assertTrue("the card came out flat, so nothing was drawn: ${colours.size} colours", colours.size > 20)
    }

    @Test
    fun everyThemeGetsItsOwnCard() {
        // The card used to have Deep Field written into it as six constants, so a player who
        // had spent a week in Ink and Paper shared a picture of an app they do not use. Each
        // card now has to come out on its own theme's background, and no two the same.
        val backgrounds = SendokuThemeId.entries.associateWith { theme ->
            val drawn = card(look = look(theme))
            drawn.getPixel(8, 8)
        }
        for ((theme, drawn) in backgrounds) {
            val expected = SendokuThemes.colors(theme, dark = true).background.toArgb()
            assertEquals("$theme drew a card that is not its own colour", expected, drawn)
        }
        assertEquals(
            "two themes share a card background, so at least one card is not its theme",
            SendokuThemeId.entries.size,
            backgrounds.values.toSet().size,
        )
    }

    @Test
    fun aLightThemeGetsALightCard() {
        // The light half of a theme is the half a card can get wrong most visibly: dark text
        // written for a dark background on paper is an unreadable picture rather than an ugly
        // one. Ink and Paper light is the case, and its card has to be light.
        val light = card(look = look(SendokuThemeId.INK, dark = false)).getPixel(8, 8)
        val dark = card(look = look(SendokuThemeId.INK, dark = true)).getPixel(8, 8)
        assertTrue("Ink light drew a dark card", android.graphics.Color.luminance(light) > 0.5f)
        assertTrue("Ink dark drew a light card", android.graphics.Color.luminance(dark) < 0.5f)
    }

    @Test
    fun theFaceOnTheCardIsTheFaceOnTheScreen() {
        // Four themes, four typefaces, and the same words drawn in each. If the card ignored
        // the face it was handed, these would be the same picture four times over.
        val words = SendokuThemeId.entries.map { theme ->
            val drawn = card(look = look(theme))
            // The band under the app name, which is text and nothing else.
            buildList {
                for (x in 230 until 800 step 3) add(drawn.getPixel(x, 130))
            }
        }
        assertEquals("two themes drew the heading identically", words.size, words.toSet().size)
    }

    @Test
    fun theCardCanBeWrittenInJapanese() {
        // The card draws its own text on a canvas rather than going through Compose, so it
        // resolves its own font. Nothing in res/font has a kana or a kanji in it, and the
        // phone's fallback is what carries Japanese here as everywhere else. If that ever
        // stopped working the card would come out with a row of empty boxes on it, which is
        // the one thing nobody would notice until it had been sent to somebody.
        val japanese = card(title = "クリア", grade = "エキスパート")
        val english = card(title = "Solved", grade = "Expert")
        var different = 0
        for (x in 0 until japanese.width step 4) {
            for (y in 0 until 260 step 4) {
                if (japanese.getPixel(x, y) != english.getPixel(x, y)) different++
            }
        }
        // The heading band is the only part that differs, and it has to differ: the two cards
        // say different words in it. Equal pixels would mean nothing was drawn either time.
        assertTrue("the Japanese card drew the same pixels as the English one", different > 200)
    }

    @Test
    fun writeOneToLookAt() {
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "cards",
        ).apply { mkdirs() }
        val cards = SendokuThemeId.entries.map { theme ->
            theme.name.lowercase() to card(look = look(theme))
        } + listOf(
            "lost" to card("Beaten by", "Beyond"),
            "japanese" to card("クリア", "エキスパート"),
            "ink_light" to card(look = look(SendokuThemeId.INK, dark = false)),
        )
        for ((name, made) in cards) {
            File(directory, "$name.png").outputStream().use { made.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
