package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NakedSingleTest {

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.NAKED_SINGLE, NakedSingle.id)
    }

    @Test
    fun `finds the only digit a cell can take`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        val cell = grid.indexOf(4, 4)
        // Leave only the 7 alive in the centre cell.
        Candidates.all(Dimensions.CLASSIC).forEach { if (it != 7) grid.eliminate(cell, it) }

        val step = assertNotNull(NakedSingle.find(grid))
        assertEquals(TechniqueId.NAKED_SINGLE, step.technique)
        assertEquals(listOf(cell), step.focusCells)
        assertEquals(listOf(CellDigit(cell, 7)), step.focusCandidates)
        assertEquals(listOf(CellDigit(cell, 7)), step.placements)
        assertEquals(emptyList(), step.eliminations)
        assertEquals(listOf(cell), step.changedCells)
    }

    @Test
    fun `finds a single that the givens create`() {
        // Row 1 holds eight digits, so the ninth cell has one candidate left.
        val board = Board.parse(Dimensions.CLASSIC, ROW_ALMOST_FULL)
        val grid = CandidateGrid.of(board)
        val step = assertNotNull(NakedSingle.find(grid))
        assertEquals(listOf(CellDigit(8, 9)), step.placements)
    }

    @Test
    fun `finds nothing on an empty grid`() {
        assertNull(NakedSingle.find(CandidateGrid.of(Board(Dimensions.CLASSIC))))
    }

    @Test
    fun `finds nothing on a solved grid`() {
        val solution = Generator(Dimensions.JUNIOR, Random(3)).completeGrid()
        assertNull(NakedSingle.find(CandidateGrid.of(solution)))
    }

    @Test
    fun `finds nothing when every empty cell keeps two or more candidates`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        grid.place(grid.indexOf(0, 0), 1)
        grid.place(grid.indexOf(1, 1), 2)
        assertNull(NakedSingle.find(grid))
    }

    @Test
    fun `never reads a cell that is already filled`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, ROW_ALMOST_FULL))
        val step = assertNotNull(NakedSingle.find(grid))
        for (placement in step.placements) {
            assertTrue(grid.isEmpty(placement.cell))
        }
    }

    @Test
    fun `applying a step writes it into the grid`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, ROW_ALMOST_FULL))
        val step = assertNotNull(NakedSingle.find(grid))
        assertTrue(grid.apply(step))
        assertEquals(9, grid.digitAt(8))
        assertFalse(grid.isEmpty(8))
    }

    @Test
    fun `applying a step does not disturb an unrelated cell`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        val cell = grid.indexOf(0, 0)
        Candidates.all(Dimensions.CLASSIC).forEach { if (it != 3) grid.eliminate(cell, it) }
        val far = grid.indexOf(8, 8)
        val before = grid.candidatesAt(far)

        grid.apply(assertNotNull(NakedSingle.find(grid)))

        assertEquals(before, grid.candidatesAt(far))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, ROW_ALMOST_FULL))
        val before = grid.copy()
        NakedSingle.find(grid)
        assertEquals(before.toBoard(), grid.toBoard())
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }

    @Test
    fun `chained singles finish a grid built from independent holes`() {
        val solution = Generator(Dimensions.CLASSIC, Random(5)).completeGrid()
        val board = solution.copy()
        // Nine cells that share no row, column or box, so each is a naked single at once.
        val holes = listOf(0 to 0, 1 to 3, 2 to 6, 3 to 1, 4 to 4, 5 to 7, 6 to 2, 7 to 5, 8 to 8)
        for ((row, col) in holes) board[row, col] = Board.EMPTY

        val grid = CandidateGrid.of(board)
        var steps = 0
        while (true) {
            val step = NakedSingle.find(grid) ?: break
            grid.apply(step)
            steps++
        }

        assertEquals(holes.size, steps)
        assertTrue(grid.isSolved)
        assertEquals(solution, grid.toBoard())
    }

    @Test
    fun `every digit it places agrees with the real solution`() {
        // The rule is only useful if it never lies, so check it against known solutions.
        // Plenty of generated puzzles offer no naked single at all, which is fine and is
        // exactly why the harder techniques exist. What is never acceptable is a wrong digit.
        var totalPlacements = 0
        repeat(20) { seed ->
            val puzzle = Generator(Dimensions.CLASSIC, Random(seed.toLong())).generate()
            val grid = CandidateGrid.of(puzzle.givens)
            while (true) {
                val step = NakedSingle.find(grid) ?: break
                for ((cell, digit) in step.placements) {
                    assertEquals(puzzle.solution.atIndex(cell), digit, "seed $seed, cell $cell")
                    totalPlacements++
                }
                grid.apply(step)
            }
            assertFalse(grid.hasContradiction, "seed $seed ended in a contradiction")
        }
        assertTrue(totalPlacements > 0, "twenty puzzles produced no naked single between them")
    }

    @Test
    fun `works on a grid shape that is not nine by nine`() {
        val solution = Generator(Dimensions.SIX, Random(2)).completeGrid()
        val board = solution.copy()
        board[0, 0] = Board.EMPTY
        val grid = CandidateGrid.of(board)
        val step = assertNotNull(NakedSingle.find(grid))
        assertEquals(solution[0, 0], step.placements.single().digit)
    }

    @Test
    fun `a deduction that changes nothing is rejected outright`() {
        assertFailsWith<IllegalArgumentException> {
            Deduction(technique = TechniqueId.NAKED_SINGLE, focusCells = listOf(0))
        }
    }

    private companion object {
        /** Row 1 is missing only its 9. Everything else is empty. */
        val ROW_ALMOST_FULL: String =
            "12345678." + ".".repeat(72)
    }
}
