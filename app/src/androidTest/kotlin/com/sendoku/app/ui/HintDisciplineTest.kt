package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintEngine
import com.sendoku.app.game.HintLevel
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * What the hint panel may and may not say at each level.
 *
 * The whole design rests on a hint being able to stop short. A panel that reveals the digit
 * on the first tap is the ordinary sudoku hint button with extra words in front of it, and
 * every decision about levels, costs and lessons would have been for nothing.
 */
class HintDisciplineTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle: RatedPuzzle by lazy {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(77))
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.first()
    }

    private fun show(level: HintLevel, onApply: () -> Unit = {}) {
        val state = GameState.start(puzzle)
        val hint = HintEngine.next(state, level) as Hint.Step
        compose.setContent {
            SendokuTheme {
                HintPanel(
                    hint = hint,
                    onMore = {},
                    onApply = onApply,
                    onDismiss = {},
                    onGlossary = {},
                    onRemoveMistake = {},
                )
            }
        }
    }

    @Test
    fun theQuietestLevelNamesARegionAndStops() {
        show(HintLevel.REGION)
        compose.onNodeWithTag("hint:region").assertIsDisplayed()
    }

    @Test
    fun theRegionLevelDoesNotOfferToDoIt() = assertNoMove(HintLevel.REGION)

    @Test
    fun namingTheTechniqueDoesNotOfferToDoIt() = assertNoMove(HintLevel.NAME)

    @Test
    fun showingTheCellsDoesNotOfferToDoIt() = assertNoMove(HintLevel.CELLS)

    /**
     * The rule that keeps this a teaching tool: the move is only ever offered next to the
     * argument for it. One level per test, because a composition can only be set once.
     */
    private fun assertNoMove(level: HintLevel) {
        var applied = false
        show(level) { applied = true }
        compose.waitForIdle()
        val doIt = compose.onAllNodesWithText(doItLabel()).fetchSemanticsNodes().size
        assertEquals("$level offered to place the digit", 0, doIt)
        assertTrue(!applied)
    }

    @Test
    fun theLastLevelOffersTheMoveAndTheReasonTogether() {
        var applied = false
        show(HintLevel.FULL) { applied = true }
        compose.onNodeWithText(doItLabel()).performClick()
        compose.waitForIdle()
        assertTrue("the last level did not offer the move at all", applied)
    }

    private fun doItLabel(): String = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        .targetContext.getString(com.sendoku.app.R.string.hint_do_it)
}
