package com.sendoku.app.game

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * A hint may never claim something the player cannot see.
 *
 * This is the bug that made the rule worth a test of its own. A hint would rule a digit out
 * of a cell, the player had written no marks there so the board looked exactly as before,
 * and the next hint would announce that the cell had only one digit left in it. On screen it
 * had two. The app was, in effect, telling somebody to guess and calling it a deduction.
 *
 * The fix is that accepting a hint leaves its work on the board: every cell it strikes ends
 * up holding its true marks. These tests walk real puzzles the way a player who only ever
 * taps the hint button does, and check the claim against what is drawn.
 */
class HintVisibilityTest {

    private fun puzzles(count: Int) = GradedGenerator(Dimensions.CLASSIC, Random(4)).let { maker ->
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.take(count).toList()
    }

    @Test
    fun `a hint that strikes a mark leaves the cell showing what is left`() {
        for (puzzle in puzzles(6)) {
            var state = GameState.start(puzzle)
            repeat(120) {
                val hint = HintEngine.next(state) as? Hint.Step ?: return@repeat
                val struck = hint.deduction.eliminations.filter { state.cells[it.cell].isEmpty }
                state = state.applyHint(hint.deduction)

                for ((cell, digit) in struck) {
                    if (!state.cells[cell].isEmpty) continue
                    assertTrue(
                        "cell $cell still shows nothing after a hint ruled $digit out of it",
                        state.cells[cell].marks.isNotEmpty,
                    )
                    assertTrue(
                        "cell $cell still shows the $digit a hint ruled out",
                        digit !in state.cells[cell].marks,
                    )
                }
            }
        }
    }

    @Test
    fun `a cell the app calls a single really does show one mark`() {
        // The player's own view of the claim "only one digit left can go here". Either the
        // board alone says so, or the marks left by an earlier hint do.
        var checked = 0
        for (puzzle in puzzles(12)) {
            var state = GameState.start(puzzle)
            repeat(200) {
                val hint = HintEngine.next(state) as? Hint.Step ?: return@repeat
                val step = hint.deduction
                if (step.technique == TechniqueId.NAKED_SINGLE) {
                    val cell = step.placements.first().cell
                    val fromBoard = CandidateGrid.of(state.toBoard()).candidatesAt(cell)
                    val shown = state.cells[cell].marks
                    checked++
                    assertTrue(
                        "the app called cell $cell a single, the board shows $fromBoard and the marks show $shown",
                        fromBoard.size == 1 || shown.size == 1,
                    )
                }
                state = state.applyHint(step)
            }
        }
        assertTrue("no single was ever hinted, so this proved nothing", checked > 50)
    }

    @Test
    fun `a hint leaning on earlier work says so`() {
        // The honesty flag. Anything the player cannot check by reading the grid has to be
        // labelled, or the panel is asserting rather than teaching.
        var flagged = 0
        for (puzzle in puzzles(8)) {
            var state = GameState.start(puzzle)
            repeat(200) {
                val hint = HintEngine.next(state) as? Hint.Step ?: return@repeat
                val relevant = hint.deduction.focusCells + hint.deduction.eliminations.map { it.cell }
                val leaning = state.eliminated.any { it.cell in relevant }
                if (leaning) {
                    flagged++
                    assertTrue("a hint used earlier work without saying so", hint.restsOnEarlierHints)
                }
                assertEquals(
                    "the flag was raised on a board that has ruled nothing out yet",
                    hint.restsOnEarlierHints && state.eliminated.isEmpty(),
                    false,
                )
                state = state.applyHint(hint.deduction)
            }
        }
        assertTrue("no hint ever leaned on earlier work, so this proved nothing", flagged > 0)
    }
}
