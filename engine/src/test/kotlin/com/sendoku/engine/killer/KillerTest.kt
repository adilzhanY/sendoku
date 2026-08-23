package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Solver
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The first slice of Killer: cages, a solver that respects them, and a generator that only
 * hands back a layout describing one grid.
 *
 * No technique ladder yet, so nothing here rates a puzzle. What these check is the part
 * everything else will stand on: that a cage means what it says, and that a generated layout
 * cannot be solved two ways.
 */
class KillerTest {

    private val dims = Dimensions.CLASSIC

    @Test
    fun `a cage holds its cells sorted, whatever order they arrive in`() {
        assertEquals(Cage.of(15, 1, 0, 2), Cage.of(15, 2, 1, 0))
    }

    @Test
    fun `a cage refuses to hold the same cell twice`() {
        val thrown = runCatching { Cage.of(9, 4, 4) }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException, "expected a refusal, got $thrown")
    }

    @Test
    fun `a puzzle refuses cages that do not cover the grid`() {
        val solution = solvedGrid()
        val thrown = runCatching {
            KillerPuzzle(dims, listOf(Cage.of(solution.atIndex(0), 0)), solution)
        }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException, "expected a refusal, got $thrown")
    }

    @Test
    fun `a puzzle refuses a cage whose sum is a lie`() {
        val solution = solvedGrid()
        val honest = eachCellItsOwnCage(solution)
        val lying = honest.toMutableList().also { it[0] = Cage(it[0].sum + 1, it[0].cells) }
        val thrown = runCatching { KillerPuzzle(dims, lying, solution) }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException, "expected a refusal, got $thrown")
    }

    @Test
    fun `a grid of one cell cages has exactly one solution, since it is the grid written out`() {
        val solution = solvedGrid()
        val puzzle = KillerPuzzle(dims, eachCellItsOwnCage(solution), solution)
        val solver = KillerSolver(dims, puzzle)

        assertTrue(solver.hasUniqueSolution())
        assertEquals(solution.toString(), solver.solve()?.toString())
    }

    @Test
    fun `the solver finds the grid the cages were drawn from`() {
        val puzzle = assertNotNull(KillerGenerator(dims, Random(4)).next(), "no puzzle was made")
        assertEquals(puzzle.solution.toString(), KillerSolver(dims, puzzle).solve()?.toString())
    }

    @Test
    fun `a generated puzzle has one solution and no more`() {
        val puzzle = assertNotNull(KillerGenerator(dims, Random(11)).next(), "no puzzle was made")
        assertEquals(1, KillerSolver(dims, puzzle).countSolutions(limit = 2))
    }

    @Test
    fun `every cell is in exactly one cage and every cage is connected`() {
        val puzzle = assertNotNull(KillerGenerator(dims, Random(23)).next(), "no puzzle was made")
        val seen = puzzle.cages.flatMap { it.cells }

        assertEquals(dims.cellCount, seen.size)
        assertEquals(dims.cellCount, seen.distinct().size)
        for (cage in puzzle.cages) {
            assertTrue(isConnected(cage), "a cage is scattered across the grid: ${cage.cells}")
        }
    }

    @Test
    fun `no cage repeats a digit, which is the rule the sums rest on`() {
        val puzzle = assertNotNull(KillerGenerator(dims, Random(77)).next(), "no puzzle was made")
        for (cage in puzzle.cages) {
            val digits = cage.cells.map { puzzle.solution.atIndex(it) }
            assertEquals(digits.size, digits.distinct().size, "a cage repeats a digit: $digits")
        }
    }

    @Test
    fun `cage sums add up to the whole grid`() {
        val puzzle = assertNotNull(KillerGenerator(dims, Random(5)).next(), "no puzzle was made")
        // Every row holds one to nine, so nine rows come to nine times forty five.
        val expected = (1..dims.size).sum() * dims.size
        assertEquals(expected, puzzle.cages.sumOf { it.sum })
    }

    @Test
    fun `cages that describe two grids are rejected rather than returned`() {
        // One cage per row is the classic ambiguity: the sums are satisfied by any arrangement
        // the columns and boxes allow, and there is always more than one.
        val solution = solvedGrid()
        val rows = (0 until dims.size).map { row ->
            val cells = (0 until dims.size).map { row * dims.size + it }
            Cage(cells.sumOf { solution.atIndex(it) }, cells)
        }
        val puzzle = KillerPuzzle(dims, rows, solution)
        assertTrue(KillerSolver(dims, puzzle).countSolutions(limit = 2) > 1)
    }

    @Test
    @Tag("slow")
    fun `twenty layouts in a row are all unique and all solvable`() {
        val maker = KillerGenerator(dims, Random(2026))
        repeat(20) { attempt ->
            val puzzle = assertNotNull(maker.next(), "attempt $attempt made nothing")
            assertTrue(
                KillerSolver(dims, puzzle).hasUniqueSolution(),
                "attempt $attempt came back with a layout that is not unique",
            )
        }
    }

    @Test
    fun `a generator that cannot draw anything says so instead of looping`() {
        // Zero attempts is the only honest way to ask for the give up path.
        assertNull(KillerGenerator(dims, Random(1)).next(attempts = 0))
    }

    private fun solvedGrid(): Board = assertNotNull(Solver(dims).solve(Board(dims), Random(99)))

    private fun eachCellItsOwnCage(solution: Board): List<Cage> =
        (0 until dims.cellCount).map { Cage.of(solution.atIndex(it), it) }

    /** True when the cage's cells are one orthogonally joined blob. */
    private fun isConnected(cage: Cage): Boolean {
        val size = dims.size
        val members = cage.cells.toSet()
        val reached = mutableSetOf(cage.cells.first())
        val queue = ArrayDeque(reached)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val row = cell / size
            val col = cell % size
            val neighbours = buildList {
                if (row > 0) add(cell - size)
                if (row < size - 1) add(cell + size)
                if (col > 0) add(cell - 1)
                if (col < size - 1) add(cell + 1)
            }
            for (next in neighbours) {
                if (next in members && reached.add(next)) queue.add(next)
            }
        }
        return reached.size == members.size
    }
}
