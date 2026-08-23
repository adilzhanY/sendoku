package com.sendoku.app.game

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * A player who only ever presses Hint must be able to finish the puzzle.
 *
 * This is the whole promise of the hint system, and it was broken. Most techniques above a
 * single do not place a digit, they rule a candidate out, and the hint engine rebuilt its
 * view of the board from the placed digits every time it was asked. So it offered the same
 * pointing pair over and over, and following hints on a Severe puzzle stopped dead at about
 * forty of eighty one cells with no way forward.
 *
 * These run the real loop to the end, which is the only way to catch it: every individual
 * hint was correct.
 */
class HintChainTest {

    private fun puzzleOf(grade: Grade, seed: Int): RatedPuzzle {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(seed))
        var made: RatedPuzzle? = null
        while (made == null || made.grade != grade) made = maker.next(Symmetry.ROTATIONAL)
        return made
    }

    /** Presses Hint and accepts it, over and over, and reports where it got to. */
    private fun followHints(start: GameState): Pair<GameState, String> {
        var state = start
        repeat(500) {
            val before = state
            when (val hint = HintEngine.next(state)) {
                is Hint.Step -> {
                    state = state.applyHint(hint.deduction)
                    if (state == before) return state to "repeated ${hint.deduction.technique} forever"
                }

                Hint.Solved -> return state to "solved"

                Hint.Stuck -> return state to "stuck"

                is Hint.Mistake -> return state to "claimed a mistake on its own work"
            }
        }
        return state to "did not finish in five hundred steps"
    }

    @Test
    fun `hints alone finish a puzzle at every grade`() {
        for (grade in Grade.entries) {
            val (state, outcome) = followHints(GameState.start(puzzleOf(grade, seed = 4242)))
            assertEquals("$grade ended: $outcome", "solved", outcome)
            assertTrue("$grade left cells empty", state.isSolved)
        }
    }

    @Test
    fun `a hand made move throws away what the hints had ruled out`() {
        var state = GameState.start(puzzleOf(Grade.SEVERE, seed = 11))
        // Walk to the first elimination, which is what fills the set.
        repeat(40) {
            val hint = HintEngine.next(state)
            if (hint is Hint.Step) state = state.applyHint(hint.deduction)
            if (state.eliminated.isNotEmpty()) return@repeat
        }
        assertTrue("no elimination was ever recorded", state.eliminated.isNotEmpty())

        val empty = state.cells.indices.first { state.cells[it].isEmpty }
        val moved = state.select(empty).enter(state.solution.atIndex(empty))
        assertTrue("a player move must not leave stale eliminations", moved.eliminated.isEmpty())
    }
}
