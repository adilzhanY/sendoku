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

class HiddenSingleTest {

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.HIDDEN_SINGLE, HiddenSingle.id)
    }

    @Test
    fun `finds the only home a digit has left in a row`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        // Strike the 5 everywhere in row 1 except its first cell.
        for (col in 1..8) grid.eliminate(grid.indexOf(0, col), 5)

        val step = assertNotNull(HiddenSingle.find(grid))
        assertEquals(TechniqueId.HIDDEN_SINGLE, step.technique)
        assertEquals(listOf(CellDigit(0, 5)), step.placements)
        assertEquals(listOf(House(HouseKind.ROW, 0)), step.houses)
    }

    @Test
    fun `finds a home a naked single would miss`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        for (col in 1..8) grid.eliminate(grid.indexOf(0, col), 5)

        // The cell still holds all nine candidates, so the cheaper rule sees nothing.
        assertEquals(9, grid.candidatesAt(0).size)
        assertNull(NakedSingle.find(grid))
        assertNotNull(HiddenSingle.find(grid))
    }

    @Test
    fun `finds the only home a digit has left in a column`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        // Kill the 5 in every row of column 3 except row 5, and spread the row damage so
        // no row is left with a single home of its own.
        for (row in 0..8) {
            if (row == 4) continue
            grid.eliminate(grid.indexOf(row, 3), 5)
        }
        val step = assertNotNull(HiddenSingle.find(grid))
        assertEquals(listOf(CellDigit(grid.indexOf(4, 3), 5)), step.placements)
        assertEquals(listOf(House(HouseKind.COLUMN, 3)), step.houses)
    }

    @Test
    fun `finds the only home a digit has left in a box`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        val box = House(HouseKind.BOX, 4)
        val keep = grid.indexOf(4, 4)
        for (cell in grid.cellsOf(box)) {
            if (cell != keep) grid.eliminate(cell, 5)
        }
        val step = assertNotNull(HiddenSingle.find(grid))
        assertEquals(listOf(CellDigit(keep, 5)), step.placements)
        assertTrue(box in step.houses)
    }

    @Test
    fun `finds nothing on an empty grid`() {
        assertNull(HiddenSingle.find(CandidateGrid.of(Board(Dimensions.CLASSIC))))
    }

    @Test
    fun `finds nothing on a solved grid`() {
        val solution = Generator(Dimensions.JUNIOR, Random(3)).completeGrid()
        assertNull(HiddenSingle.find(CandidateGrid.of(solution)))
    }

    @Test
    fun `ignores a digit that is already placed in the house`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        grid.place(grid.indexOf(0, 0), 5)
        // Placing the 5 leaves it with no home in row 1 at all, which must not be read
        // as a hidden single somewhere.
        for (col in 1..8) assertTrue(5 !in grid.candidatesAt(grid.indexOf(0, col)))
        val step = HiddenSingle.find(grid)
        if (step != null) {
            assertTrue(step.placements.none { it.digit == 5 && grid.rowOf(it.cell) == 0 })
        }
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        for (col in 1..8) grid.eliminate(grid.indexOf(0, col), 5)
        val before = grid.copy()
        HiddenSingle.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }

    @Test
    fun `singles alone crack an easy puzzle`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, EASY))
        while (true) {
            val step = NakedSingle.find(grid) ?: HiddenSingle.find(grid) ?: break
            grid.apply(step)
        }
        assertTrue(grid.isSolved, "singles left ${grid.emptyCount} cells:\n$grid")
        assertEquals(EASY_SOLUTION, grid.toBoard().toString().replace("\n", ""))
    }

    @Test
    fun `works on a grid shape that is not nine by nine`() {
        val grid = CandidateGrid.of(Board(Dimensions.SIX))
        for (col in 1..5) grid.eliminate(grid.indexOf(0, col), 4)
        val step = assertNotNull(HiddenSingle.find(grid))
        assertEquals(listOf(CellDigit(0, 4)), step.placements)
    }

    private companion object {
        const val EASY =
            "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
        const val EASY_SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}
