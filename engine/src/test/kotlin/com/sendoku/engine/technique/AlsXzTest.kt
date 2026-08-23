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

class AlsXzTest {

    private val classic = Dimensions.CLASSIC

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.ALS_XZ, AlsXz.id)
    }

    @Test
    fun `one cell and one pair, joined by a restricted digit`() {
        val grid = CandidateGrid.of(Board(classic))
        // R1C1 holds {1,2}, which is an almost locked set of one cell.
        grid.only(grid.indexOf(0, 0), 1, 2)
        // R5C1 and R5C2 hold {1,3} and {2,3}, three digits across two cells.
        grid.only(grid.indexOf(4, 0), 1, 3)
        grid.only(grid.indexOf(4, 1), 2, 3)

        val step = assertNotNull(AlsXz.find(grid))
        assertEquals(TechniqueId.ALS_XZ, step.technique)
        assertEquals(
            listOf(grid.indexOf(0, 0), grid.indexOf(4, 0), grid.indexOf(4, 1)),
            step.focusCells,
        )
        assertEquals(listOf(House(HouseKind.ROW, 0), House(HouseKind.ROW, 4)), step.houses)
        assertEquals(
            listOf(
                grid.indexOf(0, 1),
                grid.indexOf(1, 1),
                grid.indexOf(2, 1),
                grid.indexOf(3, 0),
                grid.indexOf(5, 0),
            ).map { CellDigit(it, 2) },
            step.eliminations,
        )
    }

    @Test
    fun `every struck cell sees the digit everywhere it lives in both sets`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 0), 1, 3)
        grid.only(grid.indexOf(4, 1), 2, 3)

        val step = assertNotNull(AlsXz.find(grid))
        val homes = listOf(grid.indexOf(0, 0), grid.indexOf(4, 1))
        for ((cell, digit) in step.eliminations) {
            assertEquals(2, digit)
            assertTrue(cell !in step.focusCells)
            for (home in homes) assertTrue(grid.sees(cell, home), "cell $cell misses home $home")
        }
    }

    @Test
    fun `two sets sharing only one digit prove nothing`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 0), 1, 3)
        grid.only(grid.indexOf(4, 1), 3, 4)
        assertNull(AlsXz.find(grid))
    }

    @Test
    fun `a shared digit that is not restricted proves nothing`() {
        val grid = CandidateGrid.of(Board(classic))
        // The two sets no longer share a house for either digit, so neither is restricted.
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 4), 1, 3)
        grid.only(grid.indexOf(4, 5), 2, 3)
        assertNull(AlsXz.find(grid))
    }

    @Test
    fun `finds nothing on an untouched grid`() {
        assertNull(AlsXz.find(CandidateGrid.of(Board(classic))))
    }

    @Test
    fun `finds nothing on a solved grid`() {
        assertNull(AlsXz.find(CandidateGrid.of(Generator(classic, Random(43)).completeGrid())))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(4, 0), 1, 3)
        grid.only(grid.indexOf(4, 1), 2, 3)
        val before = grid.copy()
        AlsXz.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }
}
