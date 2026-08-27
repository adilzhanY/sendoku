package com.sendoku.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
                    onBack = {},
                    onApply = onApply,
                    onDismiss = {},
                    onGlossary = {},
                    onRemoveMistake = {},
                )
            }
        }
    }

    @Test
    fun theButtonsSurviveAnExplanationTallerThanTheScreen() {
        // The panel used to grow until Close and Do it were off the bottom of the phone,
        // which left a player stuck inside it. The words scroll now; the buttons do not
        // move. Two hundred and twenty density pixels is shorter than any phone, and if it
        // holds here it holds anywhere.
        val state = GameState.start(puzzle)
        val hint = HintEngine.next(state, HintLevel.FULL) as Hint.Step
        compose.setContent {
            SendokuTheme {
                Box(Modifier.size(320.dp, 220.dp)) {
                    HintPanel(
                        hint = hint,
                        onMore = {},
                        onBack = {},
                        onApply = {},
                        onDismiss = {},
                        onGlossary = {},
                        onRemoveMistake = {},
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("hint:close").assertIsDisplayed()
        compose.onNodeWithTag("hint:apply").assertIsDisplayed()
        // And the move it is about. The reasoning may scroll, the conclusion may not: a
        // button that places a digit with the sentence naming that digit out of sight is
        // asking to be pressed on trust.
        compose.onNodeWithTag("hint:outcome").assertIsDisplayed()
    }

    @Test
    fun theFirstCardOfTheDeckHasNothingBehindIt() {
        // The arrow is there on every card so the footer never rearranges itself, which
        // means on the first card it has to be visibly dead rather than quietly useless.
        show(HintLevel.REGION)
        compose.onNodeWithTag("hint:back").assertIsNotEnabled()
    }

    @Test
    fun everyLaterCardCanBeWalkedBack() {
        var back = 0
        val state = GameState.start(puzzle)
        val hint = HintEngine.next(state, HintLevel.CELLS) as Hint.Step
        compose.setContent {
            SendokuTheme {
                HintPanel(
                    hint = hint,
                    onMore = {},
                    onBack = { back++ },
                    onApply = {},
                    onDismiss = {},
                    onGlossary = {},
                    onRemoveMistake = {},
                )
            }
        }
        compose.onNodeWithTag("hint:back").performClick()
        compose.waitForIdle()
        assertEquals("the deck would not walk backwards", 1, back)
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
