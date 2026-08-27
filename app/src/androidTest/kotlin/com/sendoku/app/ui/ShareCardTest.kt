package com.sendoku.app.ui

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The card that gets shared, drawn for real so it can be looked at. */
@RunWith(AndroidJUnit4::class)
class ShareCardTest {

    private fun card(title: String = "Solved", grade: String = "Diabolical") = ShareCard.draw(
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
        val cards = listOf(
            "won" to card(),
            "lost" to card("Beaten by", "Beyond"),
            "japanese" to card("クリア", "エキスパート"),
        )
        for ((name, made) in cards) {
            File(directory, "$name.png").outputStream().use { made.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
