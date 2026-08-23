package com.sendoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CandidateGridTest {

    @Test
    fun `an empty grid gives every cell every digit`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        assertEquals(0, grid.placedCount)
        assertEquals(81, grid.emptyCount)
        assertFalse(grid.isSolved)
        assertFalse(grid.hasContradiction)
        for (index in 0 until grid.cellCount) {
            assertEquals(Candidates.all(Dimensions.CLASSIC), grid.candidatesAt(index))
        }
    }

    @Test
    fun `a placed cell has no candidates of its own`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, EASY))
        assertEquals(5, grid[0, 0])
        assertEquals(Candidates.EMPTY, grid.candidatesAt(0, 0))
        assertFalse(grid.isEmpty(0))
    }

    @Test
    fun `candidates exclude every digit already in the row column and box`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, EASY))
        // R1C3 is empty. Its row holds 5, 3 and 7. Its column holds 8. Its box holds 6, 9 and 8.
        assertEquals(Candidates.of(1, 2, 4), grid.candidatesAt(0, 2))
    }

    @Test
    fun `placing a digit strikes it from every peer`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        val centre = grid.indexOf(4, 4)
        grid.place(centre, 7)

        assertEquals(7, grid.digitAt(centre))
        assertEquals(Candidates.EMPTY, grid.candidatesAt(centre))
        assertEquals(1, grid.placedCount)

        for (peer in grid.peersOf(centre)) {
            assertFalse(7 in grid.candidatesAt(peer), "cell $peer kept the 7")
        }
        // A cell that shares nothing with the centre keeps its full set.
        assertEquals(Candidates.all(Dimensions.CLASSIC), grid.candidatesAt(0, 0))
    }

    @Test
    fun `peers are the twenty cells that share a row column or box`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        val peers = grid.peersOf(grid.indexOf(0, 0))
        assertEquals(20, peers.size)
        assertEquals(20, peers.toSet().size)
        assertFalse(grid.indexOf(0, 0) in peers.toSet())
        assertTrue(grid.indexOf(0, 8) in peers.toSet())
        assertTrue(grid.indexOf(8, 0) in peers.toSet())
        assertTrue(grid.indexOf(2, 2) in peers.toSet())
        assertFalse(grid.indexOf(1, 3) in peers.toSet())
    }

    @Test
    fun `peers respect a non square box`() {
        // A 6 by 6 grid has boxes 3 wide and 2 tall, so a row peer set differs from a box one.
        val grid = CandidateGrid.of(Board(Dimensions.SIX))
        val peers = grid.peersOf(grid.indexOf(0, 0)).toSet()
        assertTrue(grid.indexOf(1, 2) in peers)
        assertFalse(grid.indexOf(2, 1) in peers)
        assertEquals(5 + 5 + 2, peers.size)
    }

    @Test
    fun `eliminate strikes one mark and reports whether it was there`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        assertTrue(grid.eliminate(0, 4))
        assertFalse(4 in grid.candidatesAt(0))
        assertFalse(grid.eliminate(0, 4))
        assertEquals(8, grid.candidatesAt(0).size)
    }

    @Test
    fun `an eliminated candidate stays eliminated after an unrelated placement`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        grid.eliminate(grid.indexOf(0, 0), 4)
        grid.place(grid.indexOf(8, 8), 9)
        assertFalse(4 in grid.candidatesAt(0, 0))
    }

    @Test
    fun `placing every digit solves the grid`() {
        val solution = Generator(Dimensions.JUNIOR, Random(7)).completeGrid()
        val grid = CandidateGrid.of(Board(Dimensions.JUNIOR))
        for (index in 0 until grid.cellCount) {
            grid.place(index, solution.atIndex(index))
        }
        assertTrue(grid.isSolved)
        assertEquals(0, grid.emptyCount)
        assertEquals(solution, grid.toBoard())
    }

    @Test
    fun `an empty cell with no candidates is a contradiction`() {
        val grid = CandidateGrid.of(Board(Dimensions.JUNIOR))
        val cell = grid.indexOf(0, 0)
        Candidates.all(Dimensions.JUNIOR).forEach { grid.eliminate(cell, it) }
        assertTrue(grid.hasContradiction)
    }

    @Test
    fun `a board that repeats a digit is rejected`() {
        val clash = Board(Dimensions.JUNIOR)
        clash[0, 0] = 3
        clash[0, 3] = 3
        assertNull(CandidateGrid.ofOrNull(clash))
        assertFailsWith<IllegalArgumentException> { CandidateGrid.of(clash) }
    }

    @Test
    fun `a legal board is accepted`() {
        assertNotNull(CandidateGrid.ofOrNull(Board.parse(Dimensions.CLASSIC, EASY)))
    }

    @Test
    fun `placing over a filled cell is rejected`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, EASY))
        assertFailsWith<IllegalArgumentException> { grid.place(0, 4) }
    }

    @Test
    fun `placing a digit that is not a candidate is rejected`() {
        val grid = CandidateGrid.of(Board.parse(Dimensions.CLASSIC, EASY))
        val cell = grid.indexOf(0, 2)
        assertFalse(9 in grid.candidatesAt(cell))
        assertFailsWith<IllegalArgumentException> { grid.place(cell, 9) }
    }

    @Test
    fun `a copy does not share state with the original`() {
        val grid = CandidateGrid.of(Board(Dimensions.CLASSIC))
        val copy = grid.copy()
        copy.place(0, 5)
        copy.eliminate(80, 1)

        assertEquals(0, grid.placedCount)
        assertTrue(grid.isEmpty(0))
        assertEquals(Candidates.all(Dimensions.CLASSIC), grid.candidatesAt(80))
        assertEquals(1, copy.placedCount)
    }

    @Test
    fun `toBoard round trips the placed digits`() {
        val board = Board.parse(Dimensions.CLASSIC, EASY)
        assertEquals(board, CandidateGrid.of(board).toBoard())
    }

    @Test
    fun `the derived candidates agree with a brute force check`() {
        val puzzle = Generator(Dimensions.CLASSIC, Random(11)).generate()
        val grid = CandidateGrid.of(puzzle.givens)
        val board = puzzle.givens
        for (index in 0 until grid.cellCount) {
            if (!grid.isEmpty(index)) continue
            val row = index / 9
            val col = index % 9
            val expected = (1..9).filter { digit ->
                (0 until 9).none { i -> board[row, i] == digit } &&
                    (0 until 9).none { i -> board[i, col] == digit } &&
                    (0 until 9).none { r ->
                        (0 until 9).any { c ->
                            Dimensions.CLASSIC.boxOf(r, c) == Dimensions.CLASSIC.boxOf(row, col) &&
                                board[r, c] == digit
                        }
                    }
            }
            assertEquals(expected, grid.candidatesAt(index).toList(), "cell $index")
        }
    }

    private companion object {
        const val EASY =
            "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
    }
}
