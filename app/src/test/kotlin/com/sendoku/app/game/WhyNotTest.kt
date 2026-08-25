package com.sendoku.app.game

import com.sendoku.engine.Dimensions
import com.sendoku.engine.HouseKind
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The answer to "why can this digit not go here", checked against the board it came from.
 *
 * The rule that keeps this a free answer rather than a hint: it may only ever say something
 * a player could have seen by looking. So the two things worth testing are that a reason it
 * gives is real, and that it says "nothing rules this out" rather than reaching for the
 * solution when the board is genuinely silent.
 */
class WhyNotTest {

    private val puzzle = GradedGenerator(Dimensions.CLASSIC, Random(21)).let { maker ->
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.first()
    }

    private val state = GameState.start(puzzle)

    private fun anyEmpty(): Int = state.cells.indices.first { state.cells[it].isEmpty }

    @Test
    fun `a reason names a cell that really holds the digit, in a house they really share`() {
        val cell = anyEmpty()
        var checked = 0
        for (digit in 1..9) {
            val reason = WhyNot.ask(state, cell, digit) as? WhyNot.Reason.Taken ?: continue
            checked++
            assertEquals("the named cell does not hold the digit", digit, state.cells[reason.by].digit)
            val shares = when (reason.house.kind) {
                HouseKind.ROW -> reason.by / 9 == cell / 9
                HouseKind.COLUMN -> reason.by % 9 == cell % 9
                HouseKind.BOX -> state.dims.boxOf(reason.by / 9, reason.by % 9) == state.dims.boxOf(cell / 9, cell % 9)
            }
            assertTrue("the named house does not contain both cells", shares)
        }
        assertTrue("no digit was ruled out at all, so nothing was checked", checked > 0)
    }

    @Test
    fun `a digit is ruled out exactly when the cell cannot take it`() {
        for (cell in state.cells.indices) {
            if (!state.cells[cell].isEmpty) continue
            for (digit in 1..9) {
                val possible = digit in state.candidatesAt(cell)
                val reason = WhyNot.ask(state, cell, digit)
                assertEquals(
                    "cell $cell digit $digit disagrees with the candidates",
                    possible,
                    reason == WhyNot.Reason.Possible,
                )
            }
        }
    }

    @Test
    fun `it never reaches for the answer`() {
        // The digit that belongs in a cell is often not the only one the board allows, and
        // this must say so rather than quietly pointing at the solution.
        val cell = anyEmpty()
        val truth = puzzle.puzzle.solution.atIndex(cell)
        val allowed = (1..9).filter { WhyNot.ask(state, cell, it) == WhyNot.Reason.Possible }

        assertTrue("the true digit was ruled out", truth in allowed)
        assertTrue("only the true digit was allowed, which means it leaked the answer", allowed.size > 1)
    }

    @Test
    fun `a cell that already holds a digit says so instead`() {
        val filled = state.cells.indices.first { !state.cells[it].isEmpty }
        val reason = WhyNot.ask(state, filled, 5)
        assertEquals(WhyNot.Reason.Filled(state.cells[filled].digit), reason)
    }
}
