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

class ColouringTest {

    private val classic = Dimensions.CLASSIC

    /** Strikes [digit] from every cell of [house] except [keep]. */
    private fun CandidateGrid.confine(house: House, digit: Int, vararg keep: Int) {
        for (cell in cellsOf(house)) {
            if (cell !in keep) eliminate(cell, digit)
        }
    }

    @Test
    fun `both rules report their own id`() {
        assertEquals(TechniqueId.SIMPLE_COLOURING, SimpleColouring.id)
        assertEquals(TechniqueId.MULTI_COLOURING, MultiColouring.id)
    }

    @Test
    fun `a colour that lands twice in one house is false`() {
        val grid = CandidateGrid.of(Board(classic))
        // Row 1 pins the 5 to two cells, and column 2 pins it to two more. The chain runs
        // R1C1 to R1C2 to R3C2, so the two ends share a colour, and they share box 1.
        grid.confine(House(HouseKind.ROW, 0), 5, grid.indexOf(0, 0), grid.indexOf(0, 1))
        grid.confine(House(HouseKind.COLUMN, 1), 5, grid.indexOf(0, 1), grid.indexOf(2, 1))

        val step = assertNotNull(SimpleColouring.find(grid))
        assertEquals(TechniqueId.SIMPLE_COLOURING, step.technique)
        assertEquals(
            listOf(CellDigit(grid.indexOf(0, 0), 5), CellDigit(grid.indexOf(2, 1), 5)),
            step.eliminations,
        )
        // The two struck cells really do share a house, which is the whole argument.
        assertTrue(grid.sees(grid.indexOf(0, 0), grid.indexOf(2, 1)))
    }

    @Test
    fun `a cell seeing both colours cannot hold the digit`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(House(HouseKind.BOX, 0), 5, grid.indexOf(0, 0), grid.indexOf(2, 2))
        grid.confine(House(HouseKind.ROW, 2), 5, grid.indexOf(2, 2), grid.indexOf(2, 7))
        grid.confine(House(HouseKind.COLUMN, 7), 5, grid.indexOf(2, 7), grid.indexOf(6, 7))

        val step = assertNotNull(SimpleColouring.find(grid))
        val warm = listOf(grid.indexOf(0, 0), grid.indexOf(2, 7))
        val cool = listOf(grid.indexOf(2, 2), grid.indexOf(6, 7))
        assertEquals((warm + cool).sorted(), step.focusCells)

        assertTrue(step.eliminations.isNotEmpty())
        assertTrue(CellDigit(grid.indexOf(6, 0), 5) in step.eliminations)
        for ((cell, digit) in step.eliminations) {
            assertEquals(5, digit)
            assertTrue(cell !in step.focusCells)
            assertTrue(warm.any { grid.sees(cell, it) }, "cell $cell sees no warm cell")
            assertTrue(cool.any { grid.sees(cell, it) }, "cell $cell sees no cool cell")
        }
    }

    @Test
    fun `a single strong link proves nothing on its own`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(House(HouseKind.BOX, 0), 5, grid.indexOf(0, 0), grid.indexOf(2, 2))
        assertNull(SimpleColouring.find(grid))
    }

    @Test
    fun `two chains that clash rule out the digit where their opposites meet`() {
        val grid = CandidateGrid.of(Board(classic))
        // One chain in the top left box, one in the bottom left, joined only by column 1.
        grid.confine(House(HouseKind.BOX, 0), 5, grid.indexOf(0, 0), grid.indexOf(2, 2))
        grid.confine(House(HouseKind.BOX, 6), 5, grid.indexOf(6, 0), grid.indexOf(8, 2))

        val step = assertNotNull(MultiColouring.find(grid))
        assertEquals(TechniqueId.MULTI_COLOURING, step.technique)
        assertEquals(
            listOf(3, 4, 5).map { CellDigit(grid.indexOf(it, 2), 5) },
            step.eliminations,
        )
    }

    @Test
    fun `multi colouring needs two separate chains`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(House(HouseKind.BOX, 0), 5, grid.indexOf(0, 0), grid.indexOf(2, 2))
        assertNull(MultiColouring.find(grid))
    }

    @Test
    fun `neither rule fires on an untouched grid`() {
        val grid = CandidateGrid.of(Board(classic))
        assertNull(SimpleColouring.find(grid))
        assertNull(MultiColouring.find(grid))
    }

    @Test
    fun `neither rule fires on a solved grid`() {
        val solved = CandidateGrid.of(Generator(classic, Random(17)).completeGrid())
        assertNull(SimpleColouring.find(solved))
        assertNull(MultiColouring.find(solved))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.confine(House(HouseKind.BOX, 0), 5, grid.indexOf(0, 0), grid.indexOf(2, 2))
        grid.confine(House(HouseKind.BOX, 6), 5, grid.indexOf(6, 0), grid.indexOf(8, 2))
        val before = grid.copy()
        SimpleColouring.find(grid)
        MultiColouring.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }
}
