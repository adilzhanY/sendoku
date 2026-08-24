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
        footer = "Free. No ads. No tracking.",
    )

    @Test
    fun theCardIsDrawnAtTheSizeItClaims() {
        val bitmap = card()
        assertEquals(ShareCard.WIDTH, bitmap.width)
        assertEquals(ShareCard.HEIGHT, bitmap.height)
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
    fun writeOneToLookAt() {
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "cards",
        ).apply { mkdirs() }
        for ((name, made) in listOf("won" to card(), "lost" to card("Beaten by", "Beyond"))) {
            File(directory, "$name.png").outputStream().use { made.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
