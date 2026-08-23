package com.sendoku.app.game

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hint system is the reason the technique ladder exists, so it is held to the ladder's
 * standard: it never says anything that is not true of the board in front of it.
 */
class HintTest {

    private val puzzle: RatedPuzzle by lazy {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9501))
        var made: RatedPuzzle? = null
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun game() = GameState.start(puzzle)

    @Test
    fun `a fresh puzzle always has something to suggest`() {
        val hint = HintEngine.next(game())
        assertTrue("got $hint", hint is Hint.Step)
    }

    @Test
    fun `a hint never suggests a digit that is wrong`() {
        var state = game()
        var steps = 0
        while (!state.isSolved && steps < 200) {
            val hint = HintEngine.next(state)
            if (hint !is Hint.Step) break
            for (placement in hint.deduction.placements) {
                assertEquals(
                    "the hint suggested the wrong digit",
                    state.solution.atIndex(placement.cell),
                    placement.digit,
                )
            }
            for (elimination in hint.deduction.eliminations) {
                assertTrue(
                    "the hint struck the true digit",
                    state.solution.atIndex(elimination.cell) != elimination.digit,
                )
            }
            state = state.applyHint(hint.deduction)
            steps++
        }
        assertTrue("the hints stalled after $steps steps", steps > 10)
    }

    @Test
    fun `following the hints all the way finishes the puzzle`() {
        var state = game()
        var guard = 0
        while (!state.isSolved && guard < 500) {
            val hint = HintEngine.next(state)
            if (hint !is Hint.Step) break
            state = state.applyHint(hint.deduction)
            guard++
        }
        assertTrue("the hints could not finish it, stopping at ${state.cells.count { it.isEmpty }} empty", state.isSolved)
        assertEquals(0, state.mistakes)
    }

    @Test
    fun `a wrong digit is reported before anything else`() {
        val start = game()
        val at = start.cells.indices.first { start.cells[it].isEmpty }
        val wrong = (1..9).first { it != start.solution.atIndex(at) }
        val state = start.select(at).enter(wrong)

        val hint = HintEngine.next(state)
        assertTrue("got $hint", hint is Hint.Mistake)
        assertEquals(setOf(at), (hint as Hint.Mistake).cells)
    }

    @Test
    fun `a mistake report names the cells and not the answers`() {
        val start = game()
        val at = start.cells.indices.first { start.cells[it].isEmpty }
        val state = start.select(at).enter((1..9).first { it != start.solution.atIndex(at) })
        val hint = HintEngine.next(state) as Hint.Mistake
        // It says where to look. Handing over the right digit would be the thing this app
        // exists not to do.
        assertTrue(hint.cells.isNotEmpty())
    }

    @Test
    fun `a solved puzzle has nothing left to say`() {
        var state = game()
        for (at in 0 until 81) {
            if (state.cells[at].isEmpty) state = state.select(at).enter(state.solution.atIndex(at))
        }
        assertEquals(Hint.Solved, HintEngine.next(state))
    }

    @Test
    fun `hint levels climb and then stop`() {
        assertEquals(HintLevel.CELLS, HintLevel.NAME.next)
        assertEquals(HintLevel.FULL, HintLevel.CELLS.next)
        assertEquals(HintLevel.FULL, HintLevel.FULL.next)
        assertTrue(HintLevel.NAME.hasMore)
        assertTrue(HintLevel.CELLS.hasMore)
        assertFalse(HintLevel.FULL.hasMore)
    }

    @Test
    fun `accepting a hint can be undone`() {
        val start = game()
        val hint = HintEngine.next(start) as Hint.Step
        val after = start.applyHint(hint.deduction)
        assertTrue(after.canUndo)

        var undone = after
        while (undone.canUndo) undone = undone.undo()
        assertEquals(start.cells, undone.cells)
    }

    @Test
    fun `accepting a hint does not ignore the pencil marks it strikes`() {
        var state = game()
        // Pencil in everything, so a hint that eliminates has marks to take away.
        for (at in 0 until 81) {
            if (state.cells[at].isEmpty) state = state.select(at).fillMarks()
        }
        var guard = 0
        while (guard < 60) {
            val hint = HintEngine.next(state)
            if (hint !is Hint.Step) break
            val eliminations = hint.deduction.eliminations
            val after = state.applyHint(hint.deduction)
            for ((cell, digit) in eliminations) {
                assertFalse("cell $cell kept a struck mark", digit in after.cells[cell].marks)
            }
            state = after
            guard++
        }
        assertTrue(guard > 5)
    }

}
