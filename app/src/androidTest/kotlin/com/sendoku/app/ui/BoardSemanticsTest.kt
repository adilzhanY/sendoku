package com.sendoku.app.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

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

    /**
     * The description a cell should have, written out longhand.
     *
     * Deliberately not calling the app's own describe: a test that reuses the code under test
     * to work out what it expects can only ever agree with it. These are the exact words a
     * screen reader will say, spelled out here so that changing them is a visible decision.
     */
    private fun describeFor(state: GameState, cell: Int): String {
        val position = "Row ${cell / 9 + 1}, column ${cell % 9 + 1}"
        val target = state.cells[cell]
        return when {
            target.isGiven -> "$position, ${target.digit}, a clue"
            !target.isEmpty -> "$position, ${target.digit}"
            target.marks.isNotEmpty -> "$position, empty, noted ${target.marks.toList().joinToString(", ")}"
            else -> "$position, empty"
        }
    }

    private fun padLabel(state: GameState, digit: Int): String {
        val remaining = state.remaining(digit)
        return when {
            remaining <= 0 -> "$digit, all placed"
            remaining == 1 -> "$digit, one left"
            else -> "$digit, $remaining left"
        }
    }

    @Test
    fun everyCellIsLabelledAndClickable() {
        val start = state()
        compose.setContent {
            SendokuTheme { SudokuBoard(state = start, onSelect = {}) }
        }

        for (index in 0 until 81) {
            val description = describeFor(start, index)
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

        compose.onNodeWithContentDescription(describeFor(start, clue)).assertHasClickAction()
        compose.onNodeWithContentDescription(describeFor(start, empty)).assertHasClickAction()
        assertTrue(describeFor(start, clue).endsWith("a clue"))
        assertTrue(describeFor(start, empty).endsWith("empty"))
    }

    @Test
    fun tappingACellReportsIt() {
        val start = state()
        var tapped = -1
        compose.setContent {
            SendokuTheme { SudokuBoard(state = start, onSelect = { tapped = it }) }
        }
        val empty = (0 until 81).first { start.cells[it].isEmpty }
        compose.onNodeWithContentDescription(describeFor(start, empty)).performClick()
        assertTrue("expected cell $empty, got $tapped", tapped == empty)
    }

    @Test
    fun theSelectedCellSaysItIsSelected() {
        val empty = (0 until 81).first { state().cells[it].isEmpty }
        val selected = state().select(empty)
        compose.setContent { SendokuTheme { SudokuBoard(state = selected, onSelect = {}) } }
        compose.onNodeWithContentDescription(describeFor(selected, empty)).assertIsSelected()
    }

    @Test
    fun everyPadKeySaysWhatIsLeft() {
        val start = state()
        compose.setContent {
            SendokuTheme { NumberPad(state = start, onDigit = {}) }
        }
        for (digit in 1..9) {
            compose.onNodeWithContentDescription(padLabel(start, digit)).assertHasClickAction()
        }
    }

    @Test
    fun aPencilledCellReadsOutItsMarks() {
        var start = state()
        val empty = (0 until 81).first { start.cells[it].isEmpty }
        start = start.select(empty).setPencilMode(true).enter(2).enter(7)
        compose.setContent { SendokuTheme { SudokuBoard(state = start, onSelect = {}) } }

        val description = describeFor(start, empty)
        assertTrue(description, description.contains("noted 2, 7"))
        compose.onNodeWithContentDescription(description).assertHasClickAction()
    }
}
