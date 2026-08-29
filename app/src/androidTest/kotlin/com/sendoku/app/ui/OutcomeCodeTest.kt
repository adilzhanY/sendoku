package com.sendoku.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
 * The code offered at the end of a game, in both the lengths it comes in.
 *
 * A puzzle dealt out of the shipped batch is named in five characters. One made on the phone
 * once the batch ran out, or one that arrived as a grid, is written out in full and is nearer
 * forty. Both go through the same row, and the long one used to squeeze the invitation beside
 * it down to a single letter per line running off the edge of the screen. What is checked
 * here is that the words next to the code are still readable at a sensible width.
 */
class OutcomeCodeTest {

    @get:Rule
    val compose = createComposeRule()

    private fun solved(catalogIndex: Int?): GameState {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(3))
        val made = generateSequence { maker.next(Symmetry.ROTATIONAL) }.filterNotNull().first()
        var state = GameState.start(made, catalogIndex = catalogIndex)
        for (index in state.cells.indices) {
            if (state.cells[index].isEmpty) state = state.select(index).enter(state.solution.atIndex(index))
        }
        return state
    }

    private fun show(catalogIndex: Int?) {
        val state = solved(catalogIndex)
        compose.setContent {
            SendokuTheme {
                OutcomePanel(state = state, onNextPuzzle = {}, onHome = {}, onLearn = {}, onPath = {})
            }
        }
    }

    /** The block is clickable, so its own semantics swallow its children: measure unmerged. */
    private fun widthOf(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().size.width

    @Test
    fun aGridCodeLeavesTheInvitationRoomToRead() {
        show(catalogIndex = null)
        val whole = widthOf("outcome:code")
        val label = compose.onNodeWithText("Send this puzzle to a friend", useUnmergedTree = true)
            .fetchSemanticsNode().size.width
        // Half the block is a low bar on purpose. The bug being guarded against left this
        // one character wide, and the fix gives it a line of its own.
        assertTrue("the invitation was $label wide inside $whole", label * 2 >= whole)
    }

    @Test
    fun aBatchCodeKeepsTheInvitationBesideIt() {
        show(catalogIndex = 41)
        val whole = widthOf("outcome:code")
        val label = compose.onNodeWithText("Send this puzzle to a friend", useUnmergedTree = true)
            .fetchSemanticsNode().size.width
        assertTrue("the invitation was $label wide inside $whole", label > 0 && label < whole)
    }
}
