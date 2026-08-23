package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BasicFishTest {

    private val classic = Dimensions.CLASSIC

    /** Confines [digit] to [cols] inside [row], by striking it everywhere else on the row. */
    private fun CandidateGrid.confine(row: Int, digit: Int, vararg cols: Int) {
        for (col in 0 until size) {
            if (col !in cols) eliminate(indexOf(row, col), digit)
        }
    }

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.X_WING, XWing.id)
    }

    @Test
    fun `two rows sharing two columns clear those columns everywhere else`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(row = 0, digit = 5, cols = intArrayOf(0, 4))
        grid.confine(row = 1, digit = 5, cols = intArrayOf(0, 4))

        val step = assertNotNull(XWing.find(grid))
        assertEquals(TechniqueId.X_WING, step.technique)
        assertEquals(listOf(0, 4, 9, 13), step.focusCells)
        assertEquals(
            listOf(House(HouseKind.ROW, 0), House(HouseKind.ROW, 1)) +
                listOf(House(HouseKind.COLUMN, 0), House(HouseKind.COLUMN, 4)),
            step.houses,
        )
        assertEquals(emptyList(), step.placements)
        val expected = (2..8).flatMap { row -> listOf(row * 9, row * 9 + 4) }.sorted()
        assertEquals(expected.map { CellDigit(it, 5) }, step.eliminations)
    }

    @Test
    fun `the same argument works down the columns`() {
        val grid = CandidateGrid.of(Board(classic))
        // Confine the 5 in columns 1 and 5 to rows 1 and 3, without confining any row.
        for (row in 0..8) {
            if (row == 0 || row == 2) continue
            grid.eliminate(grid.indexOf(row, 0), 5)
            grid.eliminate(grid.indexOf(row, 4), 5)
        }

        val step = assertNotNull(XWing.find(grid))
        assertEquals(listOf(0, 4, 18, 22), step.focusCells)
        assertEquals(
            listOf(House(HouseKind.COLUMN, 0), House(HouseKind.COLUMN, 4)) +
                listOf(House(HouseKind.ROW, 0), House(HouseKind.ROW, 2)),
            step.houses,
        )
        val expected = listOf(1, 2, 3, 5, 6, 7, 8).flatMap { col -> listOf(col, 18 + col) }.sorted()
        assertEquals(expected.map { CellDigit(it, 5) }, step.eliminations)
    }

    @Test
    fun `two rows on different columns are not a fish`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(row = 0, digit = 5, cols = intArrayOf(0, 4))
        grid.confine(row = 1, digit = 5, cols = intArrayOf(1, 4))
        assertNull(XWing.find(grid))
    }

    @Test
    fun `a fish that clears nothing is not reported`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(row = 0, digit = 5, cols = intArrayOf(0, 4))
        grid.confine(row = 1, digit = 5, cols = intArrayOf(0, 4))
        // Take the 5 out of both columns by hand, leaving the fish nothing to do.
        for (row in 2..8) {
            grid.eliminate(grid.indexOf(row, 0), 5)
            grid.eliminate(grid.indexOf(row, 4), 5)
        }
        assertNull(XWing.find(grid))
    }

    @Test
    fun `a row where the digit is already placed is skipped`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.place(grid.indexOf(0, 0), 5)
        // Columns 5 and 8 sit outside the box that placement just cleared, so both rows
        // really do keep two homes.
        grid.confine(row = 1, digit = 5, cols = intArrayOf(4, 7))
        grid.confine(row = 2, digit = 5, cols = intArrayOf(4, 7))

        val step = assertNotNull(XWing.find(grid))
        assertTrue(House(HouseKind.ROW, 0) !in step.houses)
        assertEquals(listOf(House(HouseKind.ROW, 1), House(HouseKind.ROW, 2)), step.houses.take(2))
    }

    @Test
    fun `finds nothing on an untouched grid`() {
        assertNull(XWing.find(CandidateGrid.of(Board(classic))))
    }

    @Test
    fun `finds nothing on a solved grid`() {
        val solved = CandidateGrid.of(Generator(Dimensions.CLASSIC, Random(12)).completeGrid())
        assertNull(XWing.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(row = 0, digit = 5, cols = intArrayOf(0, 4))
        grid.confine(row = 1, digit = 5, cols = intArrayOf(0, 4))
        val before = grid.copy()
        XWing.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }

    @Test
    fun `applying a fish strikes exactly the marks it named`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(row = 0, digit = 5, cols = intArrayOf(0, 4))
        grid.confine(row = 1, digit = 5, cols = intArrayOf(0, 4))
        val step = assertNotNull(XWing.find(grid))
        assertTrue(grid.apply(step))

        for (row in 2..8) {
            assertTrue(5 !in grid.candidatesAt(grid.indexOf(row, 0)))
            assertTrue(5 !in grid.candidatesAt(grid.indexOf(row, 4)))
            assertTrue(5 in grid.candidatesAt(grid.indexOf(row, 1)))
        }
        // The four corners keep their 5.
        for (cell in step.focusCells) assertTrue(5 in grid.candidatesAt(cell))
    }

    @Test
    fun `works on a grid shape that is not nine by nine`() {
        val grid = CandidateGrid.of(Board(Dimensions.SIX))
        grid.confine(row = 0, digit = 4, cols = intArrayOf(0, 3))
        grid.confine(row = 1, digit = 4, cols = intArrayOf(0, 3))
        val step = assertNotNull(XWing.find(grid))
        assertEquals(listOf(0, 3, 6, 9), step.focusCells)
        val expected = (2..5).flatMap { row -> listOf(row * 6, row * 6 + 3) }.sorted()
        assertEquals(expected.map { CellDigit(it, 4) }, step.eliminations)
    }
}
