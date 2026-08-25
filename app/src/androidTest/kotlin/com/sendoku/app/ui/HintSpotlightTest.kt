package com.sendoku.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * The two rules the spotlight has to keep, checked on real pixels.
 *
 * Both are the kind of thing that looks fine in code and is wrong on screen, and both are
 * about what a player can see rather than about what the state holds: an outline drawn
 * around the wrong house is still an outline, and a mistake hidden under a dim is still a
 * mistake, right up until somebody loses a game to it.
 */
class HintSpotlightTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle = GradedGenerator(Dimensions.CLASSIC, Random(88)).let { maker ->
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.first()
    }

    private fun board(state: GameState, houses: List<House>, lit: Set<Int>, spotlight: Boolean) {
        compose.setContent {
            SendokuTheme(dark = true) {
                Box(Modifier.size(360.dp)) {
                    SudokuBoard(
                        state = state,
                        onSelect = {},
                        hintLogic = lit,
                        hintHouses = houses,
                        spotlight = spotlight,
                    )
                }
            }
        }
    }

    @Test
    fun theOutlineIsDrawnAroundTheHouseTheHintNames() {
        val state = GameState.start(puzzle)
        board(state, listOf(House(HouseKind.ROW, 4)), setOf(36), spotlight = true)
        compose.waitForIdle()

        val picture = compose.onNodeWithTag("game:board").captureToImage().asAndroidBitmap()
        val cell = picture.width / 9
        // A band across the middle of row five, and the same band two rows up. The outline is
        // red, so the row it belongs to is measurably redder than the one it does not.
        assertTrue(
            "the outlined row is no redder than a row the hint never mentioned",
            redness(picture, 4 * cell, cell) > redness(picture, 2 * cell, cell) + 8,
        )
    }

    @Test
    fun noOutlineIsDrawnWhenTheHintNamesNoHouse() {
        val state = GameState.start(puzzle)
        board(state, emptyList(), setOf(36), spotlight = true)
        compose.waitForIdle()

        val picture = compose.onNodeWithTag("game:board").captureToImage().asAndroidBitmap()
        val cell = picture.width / 9
        assertTrue(
            "something red was drawn on a board whose hint names no region",
            abs(redness(picture, 4 * cell, cell) - redness(picture, 2 * cell, cell)) < 8,
        )
    }

    @Test
    fun aWrongDigitIsNeverDimmed() {
        // The one thing more urgent than the next step is the digit that makes the next step
        // pointless, so a mistake keeps its contrast whatever the hint is doing.
        val start = GameState.start(puzzle, GameSettings(autoCheck = true))
        val empty = start.cells.indices.first { start.cells[it].isEmpty }
        val wrongDigit = (1..9).first { it != start.solution.atIndex(empty) }
        val state = start.select(empty).enter(wrongDigit)

        board(state, listOf(House(HouseKind.ROW, 0)), setOf(1), spotlight = true)
        compose.waitForIdle()

        val picture = compose.onNodeWithTag("game:board").captureToImage().asAndroidBitmap()
        val cell = picture.width / 9
        val row = empty / 9
        val column = empty % 9
        val onMistake = redness(picture, row * cell + cell / 4, cell / 2, column * cell + cell / 4, cell / 2)
        assertTrue("the mistake was dimmed away to nothing", onMistake > 20)
    }

    /** How red a band of the picture is on average, over its brightness. */
    private fun redness(
        picture: android.graphics.Bitmap,
        top: Int,
        height: Int,
        left: Int = 0,
        width: Int = picture.width,
    ): Int {
        var total = 0L
        var counted = 0
        for (y in top until minOf(top + height, picture.height)) {
            for (x in left until minOf(left + width, picture.width)) {
                val pixel = picture.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val other = maxOf((pixel shr 8) and 0xFF, pixel and 0xFF)
                total += (red - other).coerceAtLeast(0)
                counted++
            }
        }
        return if (counted == 0) 0 else (total / counted).toInt()
    }
}
