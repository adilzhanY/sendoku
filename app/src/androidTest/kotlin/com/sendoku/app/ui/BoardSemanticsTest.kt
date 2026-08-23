package com.sendoku.app.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * What a screen reader is actually handed.
 *
 * This runs on a device because it has to. Dumping the view hierarchy with uiautomator shows
 * Compose's merged and unmerged trees at once and cannot answer the only question that
 * matters, which is whether the node a screen reader focuses is the node carrying the label.
 * The Compose test rule reads the merged tree, which is exactly what accessibility sees.
 */
class BoardSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle: RatedPuzzle by lazy {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9701))
        var made: RatedPuzzle? = null
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun state() = GameState.start(puzzle)

    @Test
    fun everyCellIsLabelledAndClickable() {
        val start = state()
        compose.setContent {
            SendokuTheme { SudokuBoard(state = start, onSelect = {}) }
        }

        for (index in 0 until 81) {
            val description = describe(start, index, conflicting = false)
            compose.onNodeWithContentDescription(description, useUnmergedTree = false)
                .assertHasClickAction()
        }
    }

    @Test
    fun aClueSaysSoAndAnEmptyCellSaysSo() {
        val start = state()
        compose.setContent { SendokuTheme { SudokuBoard(state = start, onSelect = {}) } }

        val clue = (0 until 81).first { start.cells[it].isGiven }
        val empty = (0 until 81).first { start.cells[it].isEmpty }

        compose.onNodeWithContentDescription(describe(start, clue, false)).assertHasClickAction()
        compose.onNodeWithContentDescription(describe(start, empty, false)).assertHasClickAction()
        assertTrue(describe(start, clue, false).endsWith("a clue"))
        assertTrue(describe(start, empty, false).endsWith("empty"))
    }

    @Test
    fun tappingACellReportsIt() {
        val start = state()
        var tapped = -1
        compose.setContent {
            SendokuTheme { SudokuBoard(state = start, onSelect = { tapped = it }) }
        }
        val empty = (0 until 81).first { start.cells[it].isEmpty }
        compose.onNodeWithContentDescription(describe(start, empty, false)).performClick()
        assertTrue("expected cell $empty, got $tapped", tapped == empty)
    }

    @Test
    fun theSelectedCellSaysItIsSelected() {
        val empty = (0 until 81).first { state().cells[it].isEmpty }
        val selected = state().select(empty)
        compose.setContent { SendokuTheme { SudokuBoard(state = selected, onSelect = {}) } }
        compose.onNodeWithContentDescription(describe(selected, empty, false)).assertIsSelected()
    }

    @Test
    fun everyPadKeySaysWhatIsLeft() {
        val start = state()
        compose.setContent {
            SendokuTheme { NumberPad(state = start, onDigit = {}) }
        }
        for (digit in 1..9) {
            val remaining = start.remaining(digit)
            val spoken = when {
                remaining <= 0 -> "$digit, all placed"
                remaining == 1 -> "$digit, one left"
                else -> "$digit, $remaining left"
            }
            compose.onNodeWithContentDescription(spoken).assertHasClickAction()
        }
    }

    @Test
    fun aPencilledCellReadsOutItsMarks() {
        var start = state()
        val empty = (0 until 81).first { start.cells[it].isEmpty }
        start = start.select(empty).setPencilMode(true).enter(2).enter(7)
        compose.setContent { SendokuTheme { SudokuBoard(state = start, onSelect = {}) } }

        val description = describe(start, empty, false)
        assertTrue(description, description.contains("noted 2, 7"))
        compose.onNodeWithContentDescription(description).assertHasClickAction()
    }
}
