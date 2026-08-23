package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UniqueRectangleTest {

    private val classic = Dimensions.CLASSIC

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    /** The rectangle used throughout: two rows, two columns, two boxes. */
    private fun corners(grid: CandidateGrid) = listOf(
        grid.indexOf(0, 0),
        grid.indexOf(0, 1),
        grid.indexOf(3, 0),
        grid.indexOf(3, 1),
    )

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.UNIQUE_RECTANGLE, UniqueRectangle.id)
    }

    @Test
    fun `type one clears the pair from the odd corner out`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2)
        grid.only(cells[3], 1, 2, 5)

        val step = assertNotNull(UniqueRectangle.find(grid))
        assertEquals(TechniqueId.UNIQUE_RECTANGLE, step.technique)
        assertEquals(cells, step.focusCells)
        assertEquals(listOf(CellDigit(cells[3], 1), CellDigit(cells[3], 2)), step.eliminations)
    }

    @Test
    fun `type one leaves the odd corner holding its extra`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2)
        grid.only(cells[3], 1, 2, 5)
        grid.apply(assertNotNull(UniqueRectangle.find(grid)))
        assertEquals(Candidates.of(5), grid.candidatesAt(cells[3]))
    }

    @Test
    fun `type two strikes the shared extra where both roof cells are seen`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2, 5)
        grid.only(cells[3], 1, 2, 5)

        val step = assertNotNull(UniqueRectangle.find(grid))
        assertTrue(step.eliminations.isNotEmpty())
        for ((cell, digit) in step.eliminations) {
            assertEquals(5, digit)
            assertTrue(cell !in cells)
            assertTrue(grid.sees(cell, cells[2]) && grid.sees(cell, cells[3]))
        }
        // Everything in the roof row and the roof box loses the 5.
        assertTrue(CellDigit(grid.indexOf(3, 8), 5) in step.eliminations)
        assertTrue(CellDigit(grid.indexOf(5, 2), 5) in step.eliminations)
    }

    @Test
    fun `type four drops the free digit when the other is locked to the roof`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2, 5)
        grid.only(cells[3], 1, 2, 6)
        // The 2 now has nowhere else to go in row 4, so it is locked to the roof.
        for (col in 2..8) grid.eliminate(grid.indexOf(3, col), 2)

        val step = assertNotNull(UniqueRectangle.find(grid))
        assertEquals(listOf(CellDigit(cells[2], 1), CellDigit(cells[3], 1)), step.eliminations)
    }

    @Test
    fun `type three locks the roof extras into a subset with a real cell`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2, 5)
        grid.only(cells[3], 1, 2, 6)
        // The roof pair behaves like one cell holding {5, 6}, which pairs up with R4C5.
        grid.only(grid.indexOf(3, 4), 5, 6)

        val step = assertNotNull(UniqueRectangle.find(grid))
        val expected = listOf(2, 3, 5, 6, 7, 8).flatMap { col ->
            listOf(CellDigit(grid.indexOf(3, col), 5), CellDigit(grid.indexOf(3, col), 6))
        }
        assertEquals(expected, step.eliminations)
    }

    @Test
    fun `four corners in four boxes are not a deadly pattern`() {
        val grid = CandidateGrid.of(Board(classic))
        // Rows 1 and 4, columns 1 and 4, which lands one corner in each of four boxes.
        val spread = listOf(
            grid.indexOf(0, 0),
            grid.indexOf(0, 3),
            grid.indexOf(3, 0),
            grid.indexOf(3, 3),
        )
        grid.only(spread[0], 1, 2)
        grid.only(spread[1], 1, 2)
        grid.only(spread[2], 1, 2)
        grid.only(spread[3], 1, 2, 5)
        assertNull(UniqueRectangle.find(grid))
    }

    @Test
    fun `a corner that already holds a digit is not part of a rectangle`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2)
        grid.place(cells[3], 1)
        assertNull(UniqueRectangle.find(grid))
    }

    @Test
    fun `finds nothing on an untouched grid`() {
        assertNull(UniqueRectangle.find(CandidateGrid.of(Board(classic))))
    }

    @Test
    fun `finds nothing on a solved grid`() {
        assertNull(UniqueRectangle.find(CandidateGrid.of(Generator(classic, Random(19)).completeGrid())))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        val cells = corners(grid)
        grid.only(cells[0], 1, 2)
        grid.only(cells[1], 1, 2)
        grid.only(cells[2], 1, 2)
        grid.only(cells[3], 1, 2, 5)
        val before = grid.copy()
        UniqueRectangle.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }
}
