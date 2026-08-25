package com.sendoku.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * A whole puzzle solved by pressing the hint button and nothing else.
 *
 * The end to end version of the promise. Every puzzle the app ships was rated by the same
 * ladder the hints come from, so a player who understands nothing and only presses the
 * button has to reach a finished grid. If this ever stops finishing, some hint in the middle
 * is offering something the board cannot carry out, and no unit test will find which one.
 *
 * The limit is off for this one game. Three hints is the rule for a player, and this is not
 * a player: it is the app checking that its own advice terminates.
 */
class HintWalkthroughTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pressingHintUntilItStopsFinishesThePuzzle() {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(303))
        val puzzle = generateSequence { maker.next(Symmetry.ROTATIONAL) }.first()
        var state by mutableStateOf(GameState.start(puzzle, GameSettings(hintLimit = null, mistakeLimit = null)))

        compose.setContent {
            SendokuTheme {
                GameScreen(
                    state = state,
                    onEvent = { state = state.reduce(it) },
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = { _ -> },
                    onSettings = {},
                    onPath = {},
                    onSpend = { _, _ -> },
                )
            }
        }

        var taps = 0
        while (!state.isSolved && taps < 400) {
            taps++
            compose.onNodeWithTag("tool:Hint").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("hint:menu:explain").performClick()
            compose.waitForIdle()
            // The panel opens at whatever the settings say, so walk it up to the move.
            repeat(3) {
                if (compose.nodeExists("hint:more")) {
                    compose.onNodeWithTag("hint:more").performClick()
                    compose.waitForIdle()
                }
            }
            if (compose.nodeExists("hint:apply")) {
                compose.onNodeWithTag("hint:apply").performClick()
                compose.waitForIdle()
            }
        }

        assertTrue("the board was not finished after $taps hints", state.isSolved)
        assertTrue("every digit placed by a hint should be right", state.mistakes == 0)
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.nodeExists(tag: String): Boolean =
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
}
