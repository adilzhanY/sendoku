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

class HiddenSubsetTest {

    private val classic = Dimensions.CLASSIC

    /** Confines [digit] to [homes] inside row 1, by striking it everywhere else in the row. */
    private fun CandidateGrid.confineInFirstRow(digit: Int, vararg homes: Int) {
        for (col in 0 until size) {
            if (col !in homes) eliminate(indexOf(0, col), digit)
        }
    }

    @Test
    fun `each rule reports its own id`() {
        assertEquals(TechniqueId.HIDDEN_PAIR, HiddenPair.id)
        assertEquals(TechniqueId.HIDDEN_TRIPLE, HiddenTriple.id)
        assertEquals(TechniqueId.HIDDEN_QUAD, HiddenQuad.id)
    }

    @Test
    fun `a hidden pair clears everything else out of its two cells`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 0, 1)

        val step = assertNotNull(HiddenPair.find(grid))
        assertEquals(TechniqueId.HIDDEN_PAIR, step.technique)
        assertEquals(listOf(0, 1), step.focusCells)
        assertEquals(listOf(House(HouseKind.ROW, 0)), step.houses)
        assertEquals(emptyList(), step.placements)
        assertEquals(
            listOf(0, 1).flatMap { cell -> (3..9).map { CellDigit(cell, it) } },
            step.eliminations,
        )
    }

    @Test
    fun `a hidden pair leaves the two digits alone`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 0, 1)
        grid.apply(assertNotNull(HiddenPair.find(grid)))

        assertEquals(Candidates.of(1, 2), grid.candidatesAt(0))
        assertEquals(Candidates.of(1, 2), grid.candidatesAt(1))
    }

    @Test
    fun `a hidden pair whose cells hold nothing else is not reported`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 0, 1)
        for (digit in 3..9) {
            grid.eliminate(0, digit)
            grid.eliminate(1, digit)
        }
        val step = HiddenPair.find(grid)
        assertTrue(step == null || step.houses != listOf(House(HouseKind.ROW, 0)))
    }

    @Test
    fun `two digits in three cells are not a hidden pair`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 1, 2)
        assertNull(HiddenPair.find(grid))
    }

    @Test
    fun `a hidden triple needs no digit to reach every cell`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 1, 2)
        grid.confineInFirstRow(3, 0, 2)

        val step = assertNotNull(HiddenTriple.find(grid))
        assertEquals(listOf(0, 1, 2), step.focusCells)
        assertEquals(
            listOf(0, 1, 2).flatMap { cell -> (4..9).map { CellDigit(cell, it) } },
            step.eliminations,
        )
    }

    @Test
    fun `a hidden triple where every digit reaches every cell`() {
        val grid = CandidateGrid.of(Board(classic))
        for (digit in 1..3) grid.confineInFirstRow(digit, 0, 1, 2)
        val step = assertNotNull(HiddenTriple.find(grid))
        assertEquals(listOf(0, 1, 2), step.focusCells)
    }

    @Test
    fun `the triple rule ignores a plain hidden pair`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 0, 1)
        assertNull(HiddenTriple.find(grid))
        assertNotNull(HiddenPair.find(grid))
    }

    @Test
    fun `a hidden quad claims four cells`() {
        val grid = CandidateGrid.of(Board(classic))
        for (digit in 1..4) grid.confineInFirstRow(digit, 0, 1, 2, 3)

        val step = assertNotNull(HiddenQuad.find(grid))
        assertEquals(listOf(0, 1, 2, 3), step.focusCells)
        assertEquals(
            listOf(0, 1, 2, 3).flatMap { cell -> (5..9).map { CellDigit(cell, it) } },
            step.eliminations,
        )
    }

    @Test
    fun `a hidden subset is found in a column and in a box too`() {
        val column = CandidateGrid.of(Board(classic))
        for (row in 0..8) {
            if (row > 1) {
                column.eliminate(column.indexOf(row, 3), 1)
                column.eliminate(column.indexOf(row, 3), 2)
            }
        }
        val fromColumn = assertNotNull(HiddenPair.find(column))
        assertEquals(listOf(House(HouseKind.COLUMN, 3)), fromColumn.houses)

        val box = CandidateGrid.of(Board(classic))
        val cells = box.cellsOf(House(HouseKind.BOX, 4))
        for (cell in cells.drop(2)) {
            box.eliminate(cell, 1)
            box.eliminate(cell, 2)
        }
        val fromBox = assertNotNull(HiddenPair.find(box))
        assertEquals(listOf(House(HouseKind.BOX, 4)), fromBox.houses)
    }

    @Test
    fun `a digit with a single home is left to the hidden single rule`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0)
        grid.confineInFirstRow(2, 0, 1)
        assertNull(HiddenPair.find(grid))
        assertNotNull(HiddenSingle.find(grid))
    }

    @Test
    fun `no hidden subset fires on an untouched grid`() {
        val grid = CandidateGrid.of(Board(classic))
        assertNull(HiddenPair.find(grid))
        assertNull(HiddenTriple.find(grid))
        assertNull(HiddenQuad.find(grid))
    }

    @Test
    fun `no hidden subset fires on a solved grid`() {
        val solved = CandidateGrid.of(Generator(Dimensions.JUNIOR, Random(8)).completeGrid())
        assertNull(HiddenPair.find(solved))
        assertNull(HiddenTriple.find(solved))
        assertNull(HiddenQuad.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 0, 1)
        val before = grid.copy()
        HiddenPair.find(grid)
        HiddenTriple.find(grid)
        HiddenQuad.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }

    @Test
    fun `works on a grid shape that is not nine by nine`() {
        val grid = CandidateGrid.of(Board(Dimensions.SIX))
        grid.confineInFirstRow(1, 0, 1)
        grid.confineInFirstRow(2, 0, 1)
        val step = assertNotNull(HiddenPair.find(grid))
        assertEquals(
            listOf(0, 1).flatMap { cell -> (3..6).map { CellDigit(cell, it) } },
            step.eliminations,
        )
    }
}
