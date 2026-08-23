package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.Solver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The promise behind every rated puzzle: it can be finished by reasoning, never by trying.
 *
 * A puzzle Sendoku rates is a puzzle Sendoku claims a person can solve. If the ladder ever
 * reached the end by luck, or by a rule that quietly assumed something, the app would be
 * shipping puzzles that cannot be honestly hinted. This is the test that says otherwise.
 */
class NoGuessingTest {

    private val classic = Dimensions.CLASSIC
    private val solver = TechniqueSolver()
    private val brute = Solver(classic)

    @Test
    fun `every puzzle the rater accepts is solvable without guessing`() {
        var rated = 0
        repeat(200) { seed ->
            val puzzle = Generator(classic, Random(20_000L + seed)).generate()
            val report = solver.solve(puzzle.givens)
            if (!report.isSolved) return@repeat
            rated++

            // The answer is the answer, not merely an answer.
            assertTrue(brute.hasUniqueSolution(puzzle.givens), "seed $seed has more than one solution")
            assertEquals(puzzle.solution, report.board, "seed $seed reached the wrong grid")

            // Every single step was forced. Replaying the path never needs a choice.
            val replay = CandidateGrid.of(puzzle.givens)
            for (step in report.steps) {
                for ((cell, digit) in step.placements) {
                    assertEquals(puzzle.solution.atIndex(cell), digit, "seed $seed guessed at cell $cell")
                }
                for ((cell, digit) in step.eliminations) {
                    assertTrue(
                        puzzle.solution.atIndex(cell) != digit,
                        "seed $seed struck the true digit at cell $cell",
                    )
                }
                assertNotNull(Techniques.byId(step.technique), "unknown technique in the path")
                replay.apply(step)
            }
            assertTrue(replay.isSolved, "seed $seed path does not finish the grid")
            assertEquals(puzzle.solution, replay.toBoard())
        }
        assertTrue(rated > 100, "only $rated of 200 puzzles were rated at all")
    }

    /** Proper puzzles with one clue removed, so they now have more than one answer. */
    private fun ambiguousBoards(): List<Board> = (0 until 40).mapNotNull { seed ->
        val puzzle = Generator(classic, Random(30_000L + seed)).generate()
        val loosened = puzzle.givens.copy()
        val clue = (0 until 81).firstOrNull { loosened.atIndex(it) != Board.EMPTY } ?: return@mapNotNull null
        loosened.setAtIndex(clue, Board.EMPTY)
        loosened.takeUnless { brute.hasUniqueSolution(it) }
    }

    @Test
    fun `pure logic never finishes a puzzle with two answers`() {
        val logic = TechniqueSolver(Techniques.logicOnly)
        val boards = ambiguousBoards()
        assertTrue(boards.isNotEmpty(), "no ambiguous puzzle was produced, so nothing was tested")
        for ((index, board) in boards.withIndex()) {
            val report = logic.solve(board)
            assertTrue(
                report.outcome == SolveOutcome.STUCK || report.outcome == SolveOutcome.CONTRADICTION,
                "board $index with two answers came back ${report.outcome} from pure logic",
            )
        }
    }

    @Test
    fun `the uniqueness rules do resolve an ambiguous puzzle, which is why they are flagged`() {
        // Not a flaw. A unique rectangle argument starts from "this puzzle has one answer",
        // so on a puzzle with two it will rule one out. The point of the test is that the
        // engine knows which rules those are.
        val full = TechniqueSolver()
        val resolved = ambiguousBoards().count { full.solve(it).isSolved }
        assertTrue(
            resolved > 0,
            "no ambiguous puzzle was resolved, so the uniqueness rules may have stopped working",
        )
        assertEquals(
            setOf(TechniqueId.UNIQUE_RECTANGLE, TechniqueId.BUG_PLUS_ONE),
            Techniques.assumesUniqueSolution,
        )
        assertEquals(
            Techniques.ladder.size - 2,
            Techniques.logicOnly.size,
        )
    }

    @Test
    fun `the solver never falls back to brute force`() {
        // A grid with a huge number of solutions must come back stuck immediately, not solved.
        val nearlyEmpty = Board(classic)
        nearlyEmpty[0, 0] = 1
        val report = solver.solve(nearlyEmpty)
        assertEquals(SolveOutcome.STUCK, report.outcome)
        assertTrue(report.steps.isEmpty())
        assertNotNull(brute.solve(nearlyEmpty), "the brute force solver can do it, which is the point")
    }
}
