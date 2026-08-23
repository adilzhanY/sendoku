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

class NakedSubsetTest {

    private val classic = Dimensions.CLASSIC

    /** Leaves [cell] holding exactly [keep], by striking everything else. */
    private fun CandidateGrid.only(cell: Int, vararg keep: Int) {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    @Test
    fun `both rules report their own id`() {
        assertEquals(TechniqueId.NAKED_PAIR, NakedPair.id)
        assertEquals(TechniqueId.NAKED_TRIPLE, NakedTriple.id)
    }

    @Test
    fun `a pair in a row clears both digits from the rest of the row`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 1, 2)

        val step = assertNotNull(NakedPair.find(grid))
        assertEquals(TechniqueId.NAKED_PAIR, step.technique)
        assertEquals(listOf(0, 1), step.focusCells)
        assertEquals(listOf(House(HouseKind.ROW, 0)), step.houses)
        assertEquals(emptyList(), step.placements)
        assertEquals((2..8).flatMap { listOf(CellDigit(it, 1), CellDigit(it, 2)) }, step.eliminations)
    }

    @Test
    fun `a pair in a column is found too`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 4), 3, 7)
        grid.only(grid.indexOf(5, 4), 3, 7)

        val step = assertNotNull(NakedPair.find(grid))
        assertEquals(listOf(House(HouseKind.COLUMN, 4)), step.houses)
        assertEquals(listOf(grid.indexOf(0, 4), grid.indexOf(5, 4)), step.focusCells)
    }

    @Test
    fun `two cells with different pairs are not a pair`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 2, 3)
        assertNull(NakedPair.find(grid))
    }

    @Test
    fun `two matching cells in different houses are not a pair`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 4), 1, 2)
        assertNull(NakedPair.find(grid))
    }

    @Test
    fun `a pair that clears nothing is not reported`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 1, 2)
        // Take the two digits out of the rest of the row by hand, leaving nothing to do.
        for (cell in 2..8) {
            grid.eliminate(cell, 1)
            grid.eliminate(cell, 2)
        }
        val step = NakedPair.find(grid)
        assertTrue(step == null || step.houses != listOf(House(HouseKind.ROW, 0)))
    }

    @Test
    fun `a triple locks three digits even when no cell holds all three`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 2, 3)
        grid.only(2, 1, 3)

        val step = assertNotNull(NakedTriple.find(grid))
        assertEquals(TechniqueId.NAKED_TRIPLE, step.technique)
        assertEquals(listOf(0, 1, 2), step.focusCells)
        assertEquals(
            (3..8).flatMap { listOf(CellDigit(it, 1), CellDigit(it, 2), CellDigit(it, 3)) },
            step.eliminations,
        )
    }

    @Test
    fun `a triple where one cell holds all three digits still counts`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2, 3)
        grid.only(1, 1, 2)
        grid.only(2, 2, 3)

        val step = assertNotNull(NakedTriple.find(grid))
        assertEquals(listOf(0, 1, 2), step.focusCells)
    }

    @Test
    fun `three cells covering four digits are not a triple`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 2, 3)
        grid.only(2, 3, 4)
        assertNull(NakedTriple.find(grid))
    }

    @Test
    fun `the triple rule ignores a plain pair`() {
        // A pair is a subset of size two, so a rule looking for three must not claim it.
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 1, 2)
        assertNull(NakedTriple.find(grid))
        assertNotNull(NakedPair.find(grid))
    }

    @Test
    fun `neither rule fires on an untouched grid`() {
        val grid = CandidateGrid.of(Board(classic))
        assertNull(NakedPair.find(grid))
        assertNull(NakedTriple.find(grid))
    }

    @Test
    fun `neither rule fires on a solved grid`() {
        val solved = CandidateGrid.of(Generator(Dimensions.JUNIOR, Random(6)).completeGrid())
        assertNull(NakedPair.find(solved))
        assertNull(NakedTriple.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 1, 2)
        val before = grid.copy()
        NakedPair.find(grid)
        NakedTriple.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }

    @Test
    fun `applying a pair actually strikes the marks`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 1, 2)
        assertTrue(grid.apply(assertNotNull(NakedPair.find(grid))))
        for (cell in 2..8) {
            assertTrue(1 !in grid.candidatesAt(cell))
            assertTrue(2 !in grid.candidatesAt(cell))
            assertEquals(7, grid.candidatesAt(cell).size)
        }
        // The pair itself keeps its own two candidates.
        assertEquals(Candidates.of(1, 2), grid.candidatesAt(0))
    }

    @Test
    fun `works on a grid shape that is not nine by nine`() {
        val grid = CandidateGrid.of(Board(Dimensions.SIX))
        grid.only(0, 1, 2)
        grid.only(1, 1, 2)
        val step = assertNotNull(NakedPair.find(grid))
        assertEquals((2..5).flatMap { listOf(CellDigit(it, 1), CellDigit(it, 2)) }, step.eliminations)
    }

    @Test
    fun `a quad locks four digits across four cells`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 2, 3)
        grid.only(2, 3, 4)
        grid.only(3, 1, 4)

        val step = assertNotNull(NakedQuad.find(grid))
        assertEquals(TechniqueId.NAKED_QUAD, step.technique)
        assertEquals(listOf(0, 1, 2, 3), step.focusCells)
        assertEquals(
            (4..8).flatMap { cell -> (1..4).map { CellDigit(cell, it) } },
            step.eliminations,
        )
    }

    @Test
    fun `the quad rule ignores a plain triple`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 2, 3)
        grid.only(2, 1, 3)
        assertNull(NakedQuad.find(grid))
        assertNotNull(NakedTriple.find(grid))
    }

    @Test
    fun `four cells covering five digits are not a quad`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(0, 1, 2)
        grid.only(1, 2, 3)
        grid.only(2, 3, 4)
        grid.only(3, 4, 5)
        assertNull(NakedQuad.find(grid))
    }
}
