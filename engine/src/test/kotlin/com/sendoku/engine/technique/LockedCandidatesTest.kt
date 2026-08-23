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

class LockedCandidatesTest {

    private val classic = Dimensions.CLASSIC

    @Test
    fun `both rules report their own id`() {
        assertEquals(TechniqueId.LOCKED_CANDIDATES_POINTING, PointingCandidates.id)
        assertEquals(TechniqueId.LOCKED_CANDIDATES_CLAIMING, ClaimingCandidates.id)
    }

    @Test
    fun `pointing clears the rest of the row the digit is locked onto`() {
        val grid = CandidateGrid.of(Board(classic))
        // In box 1 the 5 can only live in row 1, because rows 2 and 3 of the box are out.
        for (row in 1..2) {
            for (col in 0..2) grid.eliminate(grid.indexOf(row, col), 5)
        }

        val step = assertNotNull(PointingCandidates.find(grid))
        assertEquals(TechniqueId.LOCKED_CANDIDATES_POINTING, step.technique)
        assertEquals(listOf(0, 1, 2), step.focusCells)
        assertEquals(listOf(House(HouseKind.BOX, 0), House(HouseKind.ROW, 0)), step.houses)
        assertEquals((3..8).map { CellDigit(it, 5) }, step.eliminations)
        assertEquals(emptyList(), step.placements)
    }

    @Test
    fun `pointing clears the rest of the column the digit is locked onto`() {
        val grid = CandidateGrid.of(Board(classic))
        // In box 1 the 5 can only live in column 1.
        for (row in 0..2) {
            for (col in 1..2) grid.eliminate(grid.indexOf(row, col), 5)
        }

        val step = assertNotNull(PointingCandidates.find(grid))
        assertEquals(listOf(0, 9, 18), step.focusCells)
        assertEquals(listOf(House(HouseKind.BOX, 0), House(HouseKind.COLUMN, 0)), step.houses)
        assertEquals((3..8).map { CellDigit(it * 9, 5) }, step.eliminations)
    }

    @Test
    fun `pointing stays quiet when the digit is spread across the box`() {
        val grid = CandidateGrid.of(Board(classic))
        assertNull(PointingCandidates.find(grid))
    }

    @Test
    fun `pointing stays quiet when the line holds nothing left to clear`() {
        val grid = CandidateGrid.of(Board(classic))
        // Lock the 5 into row 1 of box 1, then strike it from the rest of row 1 by hand.
        for (row in 1..2) {
            for (col in 0..2) grid.eliminate(grid.indexOf(row, col), 5)
        }
        for (col in 3..8) grid.eliminate(grid.indexOf(0, col), 5)

        val step = PointingCandidates.find(grid)
        assertTrue(step == null || step.eliminations.none { it.digit == 5 && grid.rowOf(it.cell) == 0 })
    }

    @Test
    fun `claiming clears the rest of the box the digit is locked into`() {
        val grid = CandidateGrid.of(Board(classic))
        // In row 1 the 5 can only live in the first box.
        for (col in 3..8) grid.eliminate(grid.indexOf(0, col), 5)

        val step = assertNotNull(ClaimingCandidates.find(grid))
        assertEquals(TechniqueId.LOCKED_CANDIDATES_CLAIMING, step.technique)
        assertEquals(listOf(0, 1, 2), step.focusCells)
        assertEquals(listOf(House(HouseKind.ROW, 0), House(HouseKind.BOX, 0)), step.houses)
        assertEquals(listOf(9, 10, 11, 18, 19, 20).map { CellDigit(it, 5) }, step.eliminations)
    }

    @Test
    fun `claiming works from a column too`() {
        val grid = CandidateGrid.of(Board(classic))
        // In column 1 the 5 can only live in the first box.
        for (row in 3..8) grid.eliminate(grid.indexOf(row, 0), 5)
        // Keep the rows quiet so the row pass finds nothing first.
        val step = assertNotNull(ClaimingCandidates.find(grid))
        assertEquals(listOf(0, 9, 18), step.focusCells)
        assertEquals(listOf(House(HouseKind.COLUMN, 0), House(HouseKind.BOX, 0)), step.houses)
        assertEquals(listOf(1, 2, 10, 11, 19, 20).map { CellDigit(it, 5) }, step.eliminations)
    }

    @Test
    fun `claiming stays quiet when the digit spans two boxes`() {
        val grid = CandidateGrid.of(Board(classic))
        for (col in 4..8) grid.eliminate(grid.indexOf(0, col), 5)
        // The 5 now lives in columns 1 to 4 of row 1, which straddles two boxes.
        val step = ClaimingCandidates.find(grid)
        assertTrue(step == null || step.focusCells != listOf(0, 1, 2, 3))
    }

    @Test
    fun `neither rule fires on an untouched grid`() {
        val grid = CandidateGrid.of(Board(classic))
        assertNull(PointingCandidates.find(grid))
        assertNull(ClaimingCandidates.find(grid))
    }

    @Test
    fun `neither rule fires on a solved grid`() {
        val solved = CandidateGrid.of(Generator(Dimensions.JUNIOR, Random(4)).completeGrid())
        assertNull(PointingCandidates.find(solved))
        assertNull(ClaimingCandidates.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        for (col in 3..8) grid.eliminate(grid.indexOf(0, col), 5)
        val before = grid.copy()
        PointingCandidates.find(grid)
        ClaimingCandidates.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }

    @Test
    fun `pointing works where boxes are wider than they are tall`() {
        // A 6 by 6 grid has boxes 3 wide and 2 tall, so a box covers only two rows.
        val grid = CandidateGrid.of(Board(Dimensions.SIX))
        for (col in 0..2) grid.eliminate(grid.indexOf(1, col), 4)

        val step = assertNotNull(PointingCandidates.find(grid))
        assertEquals(listOf(0, 1, 2), step.focusCells)
        assertEquals(listOf(House(HouseKind.BOX, 0), House(HouseKind.ROW, 0)), step.houses)
        assertEquals(listOf(3, 4, 5).map { CellDigit(it, 4) }, step.eliminations)
    }
}
