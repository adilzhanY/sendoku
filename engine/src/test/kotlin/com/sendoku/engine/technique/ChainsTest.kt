package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
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

class ChainsTest {

    private val classic = Dimensions.CLASSIC

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    private fun CandidateGrid.confine(house: House, digit: Int, vararg keep: Int) {
        for (cell in cellsOf(house)) {
            if (cell !in keep) eliminate(cell, digit)
        }
    }

    @Test
    fun `both chains report their own id`() {
        assertEquals(TechniqueId.X_CHAIN, XChain.id)
        assertEquals(TechniqueId.XY_CHAIN, XYChain.id)
    }

    @Test
    fun `a skyscraper is a three link x chain`() {
        val grid = CandidateGrid.of(Board(classic))
        // Two columns each pin the 5 to two rows, and the two lower ends share a row.
        grid.confine(House(HouseKind.COLUMN, 0), 5, grid.indexOf(0, 0), grid.indexOf(4, 0))
        grid.confine(House(HouseKind.COLUMN, 4), 5, grid.indexOf(1, 4), grid.indexOf(4, 4))

        val step = assertNotNull(XChain.find(grid))
        assertEquals(TechniqueId.X_CHAIN, step.technique)
        assertTrue(step.eliminations.isNotEmpty())
        val ends = listOf(step.focusCells.first(), step.focusCells.last())
        for ((cell, digit) in step.eliminations) {
            assertEquals(5, digit)
            assertTrue(cell !in step.focusCells)
            assertTrue(grid.sees(cell, ends[0]) && grid.sees(cell, ends[1]))
        }
    }

    @Test
    fun `a chain of one strong link proves nothing`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(House(HouseKind.COLUMN, 0), 5, grid.indexOf(0, 0), grid.indexOf(4, 0))
        assertNull(XChain.find(grid))
    }

    @Test
    fun `an x chain finds nothing when no digit has a strong link`() {
        assertNull(XChain.find(CandidateGrid.of(Board(classic))))
    }

    @Test
    fun `an xy chain of three cells strikes where its ends are seen`() {
        val grid = CandidateGrid.of(Board(classic))
        // R1C1 {1,2}, R1C5 {2,3}, R5C5 {3,1}. If R1C1 is not 1 it is 2, so R1C5 is 3,
        // so R5C5 is 1. Either end holds the 1.
        val head = grid.indexOf(0, 0)
        val middle = grid.indexOf(0, 4)
        val tail = grid.indexOf(4, 4)
        grid.only(head, 1, 2)
        grid.only(middle, 2, 3)
        grid.only(tail, 3, 1)

        val step = assertNotNull(XYChain.find(grid))
        assertEquals(TechniqueId.XY_CHAIN, step.technique)
        assertEquals(listOf(head, middle, tail), step.focusCells)
        assertEquals(listOf(CellDigit(grid.indexOf(4, 0), 1)), step.eliminations)
    }

    @Test
    fun `an xy chain needs its ends to share a digit`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(0, 4), 2, 3)
        grid.only(grid.indexOf(4, 4), 3, 4)
        assertNull(XYChain.find(grid))
    }

    @Test
    fun `an xy chain needs its cells to see each other in turn`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(5, 7), 2, 3)
        grid.only(grid.indexOf(4, 4), 3, 1)
        assertNull(XYChain.find(grid))
    }

    @Test
    fun `neither chain fires on a solved grid`() {
        val solved = CandidateGrid.of(Generator(classic, Random(41)).completeGrid())
        assertNull(XChain.find(solved))
        assertNull(XYChain.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(0, 4), 2, 3)
        grid.only(grid.indexOf(4, 4), 3, 1)
        val before = grid.copy()
        XChain.find(grid)
        XYChain.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }
}
