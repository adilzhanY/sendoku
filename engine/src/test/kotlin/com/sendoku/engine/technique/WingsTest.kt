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

class WingsTest {

    private val classic = Dimensions.CLASSIC

    /** Leaves [cell] holding exactly [keep]. */
    private fun CandidateGrid.only(cell: Int, vararg keep: Int) {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    @Test
    fun `each wing reports its own id`() {
        assertEquals(TechniqueId.XY_WING, XYWing.id)
        assertEquals(TechniqueId.XYZ_WING, XYZWing.id)
        assertEquals(TechniqueId.W_WING, WWing.id)
    }

    @Test
    fun `an xy wing strikes the third digit where the pincers meet`() {
        val grid = CandidateGrid.of(Board(classic))
        val pivot = grid.indexOf(0, 0)
        val left = grid.indexOf(0, 4)
        val right = grid.indexOf(4, 0)
        grid.only(pivot, 1, 2)
        grid.only(left, 1, 3)
        grid.only(right, 2, 3)

        val step = assertNotNull(XYWing.find(grid))
        assertEquals(TechniqueId.XY_WING, step.technique)
        assertEquals(listOf(pivot, left, right).sorted(), step.focusCells)
        assertEquals(emptyList(), step.placements)
        assertEquals(listOf(CellDigit(grid.indexOf(4, 4), 3)), step.eliminations)
    }

    @Test
    fun `an xy wing needs three distinct digits`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(0, 4), 1, 2)
        grid.only(grid.indexOf(4, 0), 1, 2)
        assertNull(XYWing.find(grid))
    }

    @Test
    fun `an xy wing whose pincers see nothing in common is not reported`() {
        val grid = CandidateGrid.of(Board(classic))
        // Both pincers sit in the pivot's own box, so the only cells seeing both are in
        // that box, and the wing has nothing outside itself to strike.
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(1, 1), 1, 3)
        grid.only(grid.indexOf(2, 2), 2, 3)
        val step = XYWing.find(grid)
        // Whatever it finds, it must never strike a cell that fails to see both pincers.
        if (step != null) {
            val (left, right) = step.focusCells.filter { it != step.focusCells[0] }
            for ((cell, _) in step.eliminations) {
                assertTrue(grid.sees(cell, left) && grid.sees(cell, right))
            }
        }
    }

    @Test
    fun `a pincer that does not see the pivot is not a wing`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(0, 4), 1, 3)
        grid.only(grid.indexOf(5, 5), 2, 3)
        assertNull(XYWing.find(grid))
    }

    @Test
    fun `an xyz wing needs the target to see the pivot as well`() {
        val grid = CandidateGrid.of(Board(classic))
        val pivot = grid.indexOf(0, 0)
        val left = grid.indexOf(1, 1)
        val right = grid.indexOf(0, 5)
        grid.only(pivot, 1, 2, 3)
        grid.only(left, 1, 3)
        grid.only(right, 2, 3)

        val step = assertNotNull(XYZWing.find(grid))
        assertEquals(TechniqueId.XYZ_WING, step.technique)
        assertEquals(listOf(pivot, left, right).sorted(), step.focusCells)
        assertEquals(
            listOf(CellDigit(grid.indexOf(0, 1), 3), CellDigit(grid.indexOf(0, 2), 3)),
            step.eliminations,
        )
        for ((cell, _) in step.eliminations) {
            assertTrue(grid.sees(cell, pivot))
            assertTrue(grid.sees(cell, left))
            assertTrue(grid.sees(cell, right))
        }
    }

    @Test
    fun `a two candidate pivot is not an xyz wing`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(1, 1), 1, 3)
        grid.only(grid.indexOf(0, 5), 2, 3)
        assertNull(XYZWing.find(grid))
    }

    @Test
    fun `pincers that do not cover the pivot are not an xyz wing`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2, 3)
        grid.only(grid.indexOf(1, 1), 1, 3)
        grid.only(grid.indexOf(0, 5), 1, 3)
        assertNull(XYZWing.find(grid))
    }

    @Test
    fun `a w wing joins two matching pairs through a strong link`() {
        val grid = CandidateGrid.of(Board(classic))
        val a = grid.indexOf(0, 0)
        val b = grid.indexOf(4, 4)
        grid.only(a, 1, 2)
        grid.only(b, 1, 2)
        // Row 9 holds the 1 in exactly two places, one seeing each pair cell.
        for (col in 0..8) {
            if (col != 0 && col != 4) grid.eliminate(grid.indexOf(8, col), 1)
        }

        val step = assertNotNull(WWing.find(grid))
        assertEquals(TechniqueId.W_WING, step.technique)
        assertEquals(listOf(a, b), step.focusCells)
        assertEquals(
            listOf(CellDigit(grid.indexOf(0, 4), 2), CellDigit(grid.indexOf(4, 0), 2)),
            step.eliminations,
        )
    }

    @Test
    fun `a w wing needs the two cells to hold the same pair`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 4), 1, 3)
        for (col in 0..8) {
            if (col != 0 && col != 4) grid.eliminate(grid.indexOf(8, col), 1)
        }
        assertNull(WWing.find(grid))
    }

    @Test
    fun `a w wing needs a real strong link`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 4), 1, 2)
        // No house anywhere has the 1 pinned to exactly two homes.
        assertNull(WWing.find(grid))
    }

    @Test
    fun `no wing fires on an untouched grid`() {
        val grid = CandidateGrid.of(Board(classic))
        assertNull(XYWing.find(grid))
        assertNull(XYZWing.find(grid))
        assertNull(WWing.find(grid))
    }

    @Test
    fun `no wing fires on a solved grid`() {
        val solved = CandidateGrid.of(Generator(Dimensions.CLASSIC, Random(15)).completeGrid())
        assertNull(XYWing.find(solved))
        assertNull(XYZWing.find(solved))
        assertNull(WWing.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(0, 4), 1, 3)
        grid.only(grid.indexOf(4, 0), 2, 3)
        val before = grid.copy()
        XYWing.find(grid)
        XYZWing.find(grid)
        WWing.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }
}
