package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

/**
 * The bar above the board, and where the keys ended up.
 *
 * The header used to be two rows: a title nobody needed above four labelled figures, one of
 * which never changes during a game. It is one row now, and the two counters are dots. The
 * slack that came free, along with the quarter of a tall screen that used to sit empty under
 * the keys, is now between the board and the tools, which puts the keys against the bottom
 * of the screen where a thumb can reach them.
 */
class GameHeaderTest {

    @get:Rule
    val compose = createComposeRule()

    private fun game(settings: GameSettings = GameSettings(), mistakes: Int = 0): GameState {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(31))
        var made = maker.next(Symmetry.ROTATIONAL)
        while (made == null || made.grade != Grade.TRICKY) made = maker.next(Symmetry.ROTATIONAL)
        var state = GameState.start(made).copy(settings = settings).tick(11.minutes)
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }
        repeat(mistakes) { index ->
            val cell = empties[index]
            val right = state.solution.atIndex(cell)
            state = state.select(cell).enter(if (right == 9) 1 else right + 1)
        }
        return state
    }

    private fun show(state: GameState) {
        compose.setContent {
            SendokuTheme {
                GameScreen(
                    state = state,
                    onEvent = {},
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = { _ -> },
                    onSettings = {},
                    onPath = {},
                    onSpend = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun theBarSaysTheLevelAndTheClockAndNothingElseInWords() {
        show(game())
        compose.onNodeWithText("Hard").assertIsDisplayed()
        compose.onNodeWithText("11:00").assertIsDisplayed()
        // The four labelled figures are gone, and so is the app's own name.
        for (word in listOf("Difficulty", "Sendoku", "0 of 3")) {
            assertEquals(
                "$word is still in the bar",
                0,
                compose.onAllNodesWithText(word).fetchSemanticsNodes().size,
            )
        }
    }

    @Test
    fun theCountersAreStillSpokenInFull() {
        // Dots are for the eye. A screen reader still hears the sum, because three dots with
        // one lit is not something that can be read aloud.
        show(game(mistakes = 1))
        compose.onNodeWithContentDescription("Mistakes, 1 of 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hints, 0 of 3").assertIsDisplayed()
    }

    @Test
    fun withNoLimitACounterSaysNothingUntilThereIsSomethingToSay() {
        val open = GameSettings(mistakeLimit = null, hintLimit = null)
        show(game(settings = open))
        assertEquals(
            "an empty counter is on screen",
            0,
            compose.onAllNodesWithText("0").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun theKeysAreAtTheBottomOfTheScreen() {
        show(game())
        val screen = compose.onRoot().fetchSemanticsNode().size.height
        val key = compose.onNodeWithTag("pad:5").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the keys stop ${screen - key.bottom.toInt()} pixels short of the bottom",
            key.bottom > screen * 0.88f,
        )
    }

    @Test
    fun theBoardIsStillAtTheTop() {
        // The slack belongs between the board and the tools, not above the board.
        show(game())
        val screen = compose.onRoot().fetchSemanticsNode().size.height
        val board = compose.onNodeWithTag("game:board").fetchSemanticsNode().boundsInRoot
        assertTrue("the board has drifted down the screen", board.top < screen * 0.15f)
    }
}
