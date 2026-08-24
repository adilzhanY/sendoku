package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.Solver
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The fork, and the one thing it must never do: keep something that is only true down one
 * branch.
 *
 * This is the rule with the most room to be quietly wrong, because it reaches a conclusion
 * without a pattern to check it against. So the tests are about the conclusion rather than
 * the shape: what it strikes has to be impossible, and what it places has to be the answer.
 */
class ForcingChainTest {

    private val classic = Dimensions.CLASSIC

    private fun blank() = CandidateGrid.of(Board(classic))

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) = apply {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    private fun rc(row: Int, col: Int) = row * 9 + col

    /** Assume the 1 at r1c1 and three forced digits later r5c1 has nothing left. */
    private fun deadEnd() = blank()
        .only(rc(0, 0), 1, 2).only(rc(0, 4), 1, 4)
        .only(rc(4, 4), 4, 5).only(rc(4, 0), 1, 5)

    @Test
    fun `a branch that runs the grid dry rules its digit out`() {
        val step = assertNotNull(ForcingChain.find(deadEnd()))

        assertEquals(listOf(CellDigit(rc(0, 0), 1)), step.eliminations)
        assertTrue(step.placements.isEmpty(), "a dead branch proves an elimination, not a placement")
    }

    @Test
    fun `a cell with more than two possibilities is left alone`() {
        // Three branches is three times the work for a rule that already costs the most,
        // and a puzzle needing one is rarer than the app will ever see.
        val grid = blank()
            .only(rc(0, 0), 1, 2, 3).only(rc(0, 4), 1, 4)
            .only(rc(4, 4), 4, 5).only(rc(4, 0), 1, 5)

        assertNull(ForcingChain.find(grid))
    }

    @Test
    fun `an untouched grid gives it nothing to work with`() {
        assertNull(ForcingChain.find(blank()))
    }

    @Tag("slow")
    @Test
    fun `on real puzzles it never contradicts the answer`() {
        // The rule reaches its conclusion by assuming things, so the only test that really
        // matters is whether the conclusion survives contact with the solution.
        var fired = 0
        for (seed in 0 until 1500) {
            val puzzle = Generator(classic, Random(40_000L + seed)).generate()
            val grid = CandidateGrid.of(puzzle.givens)
            val cheaper = Techniques.ladder.filter { it.id.cost < TechniqueId.FORCING_CHAIN.cost }

            while (true) {
                val cheap = cheaper.firstNotNullOfOrNull { it.find(grid) }
                if (cheap != null) {
                    grid.apply(cheap)
                    continue
                }
                val step = ForcingChain.find(grid) ?: break
                fired++
                for ((cell, digit) in step.placements) {
                    assertEquals(puzzle.solution.atIndex(cell), digit, "the fork placed the wrong digit, seed $seed")
                }
                for ((cell, digit) in step.eliminations) {
                    assertTrue(
                        puzzle.solution.atIndex(cell) != digit,
                        "the fork struck the true digit at cell $cell, seed $seed",
                    )
                }
                grid.apply(step)
            }
        }
        assertTrue(fired >= 8, "the fork only fired $fired times, so this proved almost nothing")
    }

    @Tag("slow")
    @Test
    fun `it cannot finish a puzzle that has two answers`() {
        // The guard against the rule quietly turning into a guess. On an ambiguous grid an
        // honest fork can never choose between the answers, so it must stop short.
        val brute = Solver(classic)
        val solver = TechniqueSolver(Techniques.logicOnly)
        var tested = 0
        for (seed in 0 until 40) {
            val puzzle = Generator(classic, Random(50_000L + seed)).generate()
            val loosened = puzzle.givens.copy()
            val clue = (0 until 81).first { loosened.atIndex(it) != Board.EMPTY }
            loosened.setAtIndex(clue, Board.EMPTY)
            if (brute.hasUniqueSolution(loosened)) continue

            tested++
            assertTrue(!solver.solve(loosened).isSolved, "seed $seed with two answers was finished anyway")
        }
        assertTrue(tested > 0, "no ambiguous puzzle was produced, so nothing was tested")
    }
}
