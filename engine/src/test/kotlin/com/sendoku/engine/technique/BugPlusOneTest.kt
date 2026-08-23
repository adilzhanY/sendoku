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

class BugPlusOneTest {

    private val classic = Dimensions.CLASSIC

    private fun next(digit: Int) = digit % 9 + 1

    /**
     * Builds a bivalue universal grave on an empty grid.
     *
     * Two complete solutions that agree nowhere, laid on top of each other, give every
     * cell exactly two candidates and every digit exactly twice in every house. Relabelling
     * a solution by a nine cycle produces a second solution that shares no cell with the
     * first, which is the cheapest way to get one.
     *
     * Real grids reach this state through elimination rather than construction, and rarely.
     * Building it directly is the only practical way to test the rule.
     */
    private fun grave(seed: Int): Pair<CandidateGrid, Board> {
        val solution = Generator(classic, Random(seed.toLong())).completeGrid()
        val grid = CandidateGrid.of(Board(classic))
        for (cell in 0 until grid.cellCount) {
            val digit = solution.atIndex(cell)
            val pair = Candidates.of(digit, next(digit))
            Candidates.all(classic).forEach { if (it !in pair) grid.eliminate(cell, it) }
        }
        return grid to solution
    }

    @Test
    fun `reports its own id`() {
        assertEquals(TechniqueId.BUG_PLUS_ONE, BugPlusOne.id)
    }

    @Test
    fun `a plain grave proves nothing, since every cell is bivalue`() {
        val (grid, _) = grave(31)
        assertNull(BugPlusOne.find(grid))
    }

    @Test
    fun `the one candidate that escapes the grave must be true`() {
        val (grid, solution) = grave(31)
        val odd = 40
        val digit = solution.atIndex(odd)
        val extra = next(next(digit))
        // Put the third candidate back, which is what a real grid would have kept all along.
        val keep = Candidates.of(digit, next(digit), extra)
        val rebuilt = CandidateGrid.of(Board(classic))
        for (cell in 0 until rebuilt.cellCount) {
            val wanted = if (cell == odd) keep else grid.candidatesAt(cell)
            Candidates.all(classic).forEach { if (it !in wanted) rebuilt.eliminate(cell, it) }
        }

        val step = assertNotNull(BugPlusOne.find(rebuilt))
        assertEquals(TechniqueId.BUG_PLUS_ONE, step.technique)
        assertEquals(listOf(CellDigit(odd, extra)), step.placements)
        assertEquals(listOf(odd), step.focusCells)
        assertEquals(rebuilt.housesOf(odd), step.houses)
    }

    @Test
    fun `two cells escaping the grave is not a grave plus one`() {
        val (grid, solution) = grave(31)
        val rebuilt = CandidateGrid.of(Board(classic))
        for (cell in 0 until rebuilt.cellCount) {
            val wanted = if (cell == 40 || cell == 41) {
                val digit = solution.atIndex(cell)
                Candidates.of(digit, next(digit), next(next(digit)))
            } else {
                grid.candidatesAt(cell)
            }
            Candidates.all(classic).forEach { if (it !in wanted) rebuilt.eliminate(cell, it) }
        }
        assertNull(BugPlusOne.find(rebuilt))
    }

    @Test
    fun `a grid that is nowhere near a grave is left alone`() {
        assertNull(BugPlusOne.find(CandidateGrid.of(Board(classic))))
        val puzzle = Generator(classic, Random(33)).generate()
        assertNull(BugPlusOne.find(CandidateGrid.of(puzzle.givens)))
    }

    @Test
    fun `a solved grid is not a grave`() {
        assertNull(BugPlusOne.find(CandidateGrid.of(Generator(classic, Random(35)).completeGrid())))
    }

    @Test
    fun `a near miss on the counts is refused`() {
        // One extra elimination somewhere else breaks the twice per house property, and the
        // rule must not place anything on a pattern that only looks right.
        val (grid, solution) = grave(31)
        val odd = 40
        val extra = next(next(solution.atIndex(odd)))
        val rebuilt = CandidateGrid.of(Board(classic))
        for (cell in 0 until rebuilt.cellCount) {
            val wanted = if (cell == odd) {
                grid.candidatesAt(cell) + extra
            } else {
                grid.candidatesAt(cell)
            }
            Candidates.all(classic).forEach { if (it !in wanted) rebuilt.eliminate(cell, it) }
        }
        rebuilt.eliminate(0, grid.candidatesAt(0).lowest)
        assertNull(BugPlusOne.find(rebuilt))
    }

    @Test
    fun `finding a step does not modify the grid`() {
        val (grid, solution) = grave(31)
        val odd = 40
        val extra = next(next(solution.atIndex(odd)))
        val rebuilt = CandidateGrid.of(Board(classic))
        for (cell in 0 until rebuilt.cellCount) {
            val wanted = if (cell == odd) grid.candidatesAt(cell) + extra else grid.candidatesAt(cell)
            Candidates.all(classic).forEach { if (it !in wanted) rebuilt.eliminate(cell, it) }
        }
        val before = rebuilt.copy()
        BugPlusOne.find(rebuilt)
        for (index in 0 until rebuilt.cellCount) {
            assertEquals(before.candidatesAt(index), rebuilt.candidatesAt(index))
        }
    }
}
