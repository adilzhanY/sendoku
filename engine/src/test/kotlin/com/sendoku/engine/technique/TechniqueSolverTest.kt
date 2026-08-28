package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.Grade
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TechniqueSolverTest {

    private val classic = Dimensions.CLASSIC
    private val solver = TechniqueSolver()

    @Test
    fun `the ladder holds every technique exactly once`() {
        // Every rule except the cage ones, which can never fire on an ordinary sudoku and
        // live on the Killer ladder instead.
        val classic = TechniqueId.entries.filterNot { it.isCage }
        assertEquals(classic.size, Techniques.ladder.size)
        assertEquals(classic.toSet(), Techniques.ladder.map { it.id }.toSet())
    }

    @Test
    fun `the ladder runs cheapest first`() {
        val costs = Techniques.ladder.map { it.id.cost }
        assertEquals(costs.sorted(), costs)
        assertEquals(TechniqueId.NAKED_SINGLE, Techniques.ladder.first().id)
        assertEquals(TechniqueId.entries.filterNot { it.isCage }.maxOf { it.cost }, Techniques.ladder.last().id.cost)
    }

    @Test
    fun `every technique can be looked up by id`() {
        for (id in TechniqueId.entries.filterNot { it.isCage }) {
            assertEquals(id, assertNotNull(Techniques.byId(id)).id)
        }
    }

    @Test
    fun `an easy puzzle solves with singles and rates gentle`() {
        val report = solver.solve(Board.parse(classic, EASY))
        assertEquals(SolveOutcome.SOLVED, report.outcome)
        assertTrue(report.isSolved)
        assertEquals(EASY_SOLUTION, report.board.toString().replace("\n", ""))
        assertEquals(TechniqueId.NAKED_SINGLE, report.hardest)
        assertEquals(Grade.GENTLE, report.grade)
    }

    @Test
    fun `the solve path is complete and in order`() {
        val report = solver.solve(Board.parse(classic, EASY))
        // Replaying the path from the givens must land on exactly the same grid.
        val replay = com.sendoku.engine.CandidateGrid.of(Board.parse(classic, EASY))
        for (step in report.steps) replay.apply(step)
        assertEquals(report.board, replay.toBoard())
        assertEquals(report.steps.size, report.usage.values.sum())
    }

    @Test
    fun `an already solved grid needs no steps at all`() {
        val solved = Generator(classic, Random(51)).completeGrid()
        val report = solver.solve(solved)
        assertEquals(SolveOutcome.SOLVED, report.outcome)
        assertEquals(emptyList(), report.steps)
        assertEquals(0.0, report.rating)
        assertNull(report.hardest)
    }

    @Test
    fun `a board that repeats a digit is refused outright`() {
        val clash = Board(classic)
        clash[0, 0] = 5
        clash[0, 4] = 5
        val report = solver.solve(clash)
        assertEquals(SolveOutcome.ILLEGAL, report.outcome)
        assertFalse(report.isSolved)
        assertEquals(emptyList(), report.steps)
    }

    @Test
    fun `a board with a dead cell reports a contradiction`() {
        // The top left cell has no candidate left: its row holds 2 to 9 and its column the 1.
        val report = solver.solve(Board.parse(classic, DEAD_CELL))
        assertEquals(SolveOutcome.CONTRADICTION, report.outcome)
    }

    @Test
    fun `a puzzle beyond the ladder comes back stuck, never guessed`() {
        // An empty grid has no forced move anywhere, so every rule must decline.
        val report = solver.solve(Board(classic))
        assertEquals(SolveOutcome.STUCK, report.outcome)
        assertEquals(emptyList(), report.steps)
        assertFalse(report.board.isFull)
        assertNull(solver.rate(Board(classic)))
        assertNull(solver.grade(Board(classic)))
    }

    @Test
    fun `whatever it solves, it solves correctly`() {
        var solved = 0
        repeat(60) { seed ->
            val puzzle = Generator(classic, Random(6000L + seed)).generate()
            val report = solver.solve(puzzle.givens)
            if (report.outcome == SolveOutcome.SOLVED) {
                assertEquals(puzzle.solution, report.board, "seed $seed solved to the wrong grid")
                solved++
            } else {
                assertEquals(SolveOutcome.STUCK, report.outcome, "seed $seed")
            }
        }
        assertTrue(solved > 40, "only $solved of 60 fell to the whole ladder")
    }

    @Test
    fun `rating is the hardest rule used`() {
        val single = listOf(step(TechniqueId.NAKED_SINGLE))
        assertEquals(1.0, TechniqueSolver.ratingOf(single))

        val mixed = listOf(
            step(TechniqueId.NAKED_SINGLE),
            step(TechniqueId.X_WING),
            step(TechniqueId.HIDDEN_SINGLE),
        )
        assertEquals(TechniqueId.X_WING.cost, TechniqueSolver.ratingOf(mixed))
    }

    @Test
    fun `leaning on the hardest rule again adds a little`() {
        val once = TechniqueSolver.ratingOf(listOf(step(TechniqueId.X_WING)))
        val twice = TechniqueSolver.ratingOf(List(2) { step(TechniqueId.X_WING) })
        val many = TechniqueSolver.ratingOf(List(40) { step(TechniqueId.X_WING) })
        assertTrue(twice > once)
        assertTrue(many > twice)
        assertTrue(abs(many - (TechniqueId.X_WING.cost + TechniqueSolver.MAX_REPETITION_BONUS)) < 1e-9)
    }

    @Test
    fun `repetition never pushes a puzzle into the next grade`() {
        for (id in TechniqueId.entries.filterNot { it.isCage }) {
            val once = Grade.of(TechniqueSolver.ratingOf(listOf(step(id))))
            val many = Grade.of(TechniqueSolver.ratingOf(List(50) { step(id) }))
            assertEquals(once, many, "${id.displayName} changes grade when repeated")
        }
    }

    @Test
    fun `an empty path rates zero`() {
        assertEquals(0.0, TechniqueSolver.ratingOf(emptyList()))
        assertEquals(Grade.GENTLE, Grade.of(0.0))
    }

    @Test
    fun `every grade is reachable and the bands do not overlap`() {
        val bounds = Grade.entries.map { it.maxRating }
        assertEquals(bounds.sorted(), bounds)
        val hardest = Grade.entries.last()
        assertEquals(hardest, Grade.of(Double.MAX_VALUE))
        for (grade in Grade.entries) {
            val inside = if (grade == hardest) 99.0 else grade.maxRating - 0.01
            assertEquals(grade, Grade.of(inside), "${grade.displayName} does not contain its own top")
        }
    }

    @Test
    fun `each technique lands in the grade its name suggests`() {
        assertEquals(Grade.GENTLE, Grade.of(TechniqueId.NAKED_SINGLE.cost))
        assertEquals(Grade.GENTLE, Grade.of(TechniqueId.HIDDEN_SINGLE.cost))
        assertEquals(Grade.STEADY, Grade.of(TechniqueId.LOCKED_CANDIDATES_POINTING.cost))
        assertEquals(Grade.STEADY, Grade.of(TechniqueId.NAKED_PAIR.cost))
        assertEquals(Grade.STEADY, Grade.of(TechniqueId.LOCKED_CANDIDATES_CLAIMING.cost))
        assertEquals(Grade.TRICKY, Grade.of(TechniqueId.X_WING.cost))
        assertEquals(Grade.TRICKY, Grade.of(TechniqueId.HIDDEN_TRIPLE.cost))
        assertEquals(Grade.SEVERE, Grade.of(TechniqueId.XY_WING.cost))
        assertEquals(Grade.SEVERE, Grade.of(TechniqueId.UNIQUE_RECTANGLE.cost))
        assertEquals(Grade.DIABOLICAL, Grade.of(TechniqueId.X_CHAIN.cost))
        assertEquals(Grade.DIABOLICAL, Grade.of(TechniqueId.XY_CHAIN.cost))
        assertEquals(Grade.BEYOND, Grade.of(TechniqueId.ALS_XZ.cost))
    }

    @Test
    fun `usage counts every step`() {
        val report = solver.solve(Board.parse(classic, EASY))
        assertEquals(report.steps.size, report.usage.values.sum())
        for ((id, count) in report.usage) {
            assertEquals(count, report.steps.count { it.technique == id }, id.displayName)
        }
    }

    @Test
    fun `a smaller ladder rates the same puzzle harder or not at all`() {
        val puzzle = Generator(classic, Random(53)).generate()
        val full = solver.solve(puzzle.givens)
        val singlesOnly = TechniqueSolver(listOf(NakedSingle, HiddenSingle)).solve(puzzle.givens)
        if (singlesOnly.isSolved) {
            assertTrue(full.isSolved)
            assertTrue(full.rating <= singlesOnly.rating + 1e-9)
        } else {
            assertEquals(SolveOutcome.STUCK, singlesOnly.outcome)
        }
    }

    private fun step(id: TechniqueId) = Deduction(technique = id, placements = listOf(CellDigit(0, 1)))

    private companion object {
        const val EASY =
            "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
        const val EASY_SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
        const val DEAD_CELL =
            ".23456789....................................1..................................."
    }
}
