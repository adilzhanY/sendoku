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

class RemotePairsTest {

    private val classic = Dimensions.CLASSIC

    private fun CandidateGrid.only(cell: Int, vararg keep: Int) {
        val kept = Candidates.of(*keep)
        Candidates.all(dims).forEach { if (it !in kept) eliminate(cell, it) }
    }

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.REMOTE_PAIRS, RemotePairs.id)
    }

    @Test
    fun `a chain of four clears both digits where the ends are seen`() {
        val grid = CandidateGrid.of(Board(classic))
        val a = grid.indexOf(0, 0)
        val b = grid.indexOf(0, 4)
        val c = grid.indexOf(4, 4)
        val d = grid.indexOf(4, 8)
        for (cell in listOf(a, b, c, d)) grid.only(cell, 1, 2)

        val step = assertNotNull(RemotePairs.find(grid))
        assertEquals(TechniqueId.REMOTE_PAIRS, step.technique)
        assertEquals(listOf(a, b, c, d).sorted(), step.focusCells)
        assertEquals(
            listOf(grid.indexOf(0, 8), grid.indexOf(4, 0)).sorted().flatMap {
                listOf(CellDigit(it, 1), CellDigit(it, 2))
            },
            step.eliminations,
        )
    }

    @Test
    fun `a chain of three is not enough`() {
        val grid = CandidateGrid.of(Board(classic))
        for (cell in listOf(grid.indexOf(0, 0), grid.indexOf(0, 4), grid.indexOf(4, 4))) {
            grid.only(cell, 1, 2)
        }
        assertNull(RemotePairs.find(grid))
    }

    @Test
    fun `cells holding different pairs are not a chain`() {
        val grid = CandidateGrid.of(Board(classic))
        grid.only(grid.indexOf(0, 0), 1, 2)
        grid.only(grid.indexOf(0, 4), 1, 3)
        grid.only(grid.indexOf(4, 4), 1, 2)
        grid.only(grid.indexOf(4, 8), 1, 2)
        assertNull(RemotePairs.find(grid))
    }

    @Test
    fun `four cells that never link up are not a chain`() {
        val grid = CandidateGrid.of(Board(classic))
        // Scattered so no two of them share a row, a column or a box.
        for (cell in listOf(
            grid.indexOf(0, 0),
            grid.indexOf(1, 4),
            grid.indexOf(5, 7),
            grid.indexOf(7, 2),
        )) {
            grid.only(cell, 1, 2)
        }
        assertNull(RemotePairs.find(grid))
    }

    @Test
    fun `every struck cell really sees both ends`() {
        val grid = CandidateGrid.of(Board(classic))
        val chain = listOf(
            grid.indexOf(0, 0),
            grid.indexOf(0, 4),
            grid.indexOf(4, 4),
            grid.indexOf(4, 8),
        )
        for (cell in chain) grid.only(cell, 1, 2)
        val step = assertNotNull(RemotePairs.find(grid))
        for ((cell, digit) in step.eliminations) {
            assertTrue(digit == 1 || digit == 2)
            assertTrue(cell !in chain)
            assertTrue(grid.sees(cell, chain[0]) && grid.sees(cell, chain[3]))
        }
    }

    @Test
    fun `finds nothing on an untouched grid`() {
        assertNull(RemotePairs.find(CandidateGrid.of(Board(classic))))
    }

    @Test
    fun `finds nothing on a solved grid`() {
        assertNull(RemotePairs.find(CandidateGrid.of(Generator(classic, Random(23)).completeGrid())))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val grid = CandidateGrid.of(Board(classic))
        for (cell in listOf(
            grid.indexOf(0, 0),
            grid.indexOf(0, 4),
            grid.indexOf(4, 4),
            grid.indexOf(4, 8),
        )) {
            grid.only(cell, 1, 2)
        }
        val before = grid.copy()
        RemotePairs.find(grid)
        for (index in 0 until grid.cellCount) {
            assertEquals(before.candidatesAt(index), grid.candidatesAt(index))
        }
    }
}
