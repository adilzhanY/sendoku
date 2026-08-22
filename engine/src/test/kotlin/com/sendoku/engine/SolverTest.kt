package com.sendoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolverTest {

    private val classic = Solver(Dimensions.CLASSIC)

    @Test
    fun `solves an ordinary puzzle`() {
        val puzzle = Board.parse(Dimensions.CLASSIC, EASY)
        val solved = assertNotNull(classic.solve(puzzle))
        assertTrue(solved.isFull)
        assertEquals(EASY_SOLUTION, solved.toString().replace("\n", ""))
    }

    @Test
    fun `solves a puzzle that needs real backtracking`() {
        val puzzle = Board.parse(Dimensions.CLASSIC, HARD_17_CLUE)
        val solved = assertNotNull(classic.solve(puzzle))
        assertTrue(solved.isFull)
        assertTrue(classic.isLegal(solved))
    }

    @Test
    fun `finds exactly one solution for a proper puzzle`() {
        val puzzle = Board.parse(Dimensions.CLASSIC, EASY)
        assertEquals(1, classic.countSolutions(puzzle))
        assertTrue(classic.hasUniqueSolution(puzzle))
    }

    @Test
    fun `finds more than one solution when a clue is missing`() {
        val puzzle = Board.parse(Dimensions.CLASSIC, EASY)
        // An empty grid has a vast number of solutions, so the count must stop at the limit.
        assertEquals(2, classic.countSolutions(Board(Dimensions.CLASSIC)))
        assertFalse(classic.hasUniqueSolution(Board(Dimensions.CLASSIC)))
        assertTrue(classic.hasUniqueSolution(puzzle))
    }

    @Test
    fun `refuses a board that already breaks a rule`() {
        val board = Board(Dimensions.CLASSIC)
        board[0, 0] = 5
        board[0, 4] = 5
        assertFalse(classic.isLegal(board))
        assertNull(classic.solve(board))
        assertEquals(0, classic.countSolutions(board))
    }

    @Test
    fun `reports an unsolvable but legal board as having no solution`() {
        val board = Board.parse(Dimensions.CLASSIC, UNSOLVABLE)
        assertTrue(classic.isLegal(board))
        assertNull(classic.solve(board))
    }

    @Test
    fun `fills every supported grid size`() {
        for (dims in listOf(Dimensions.JUNIOR, Dimensions.SIX, Dimensions.CLASSIC, Dimensions.HEXADOKU)) {
            val solved = assertNotNull(Solver(dims).solve(Board(dims), Random(7)))
            assertTrue(solved.isFull, "grid ${dims.size} was not filled")
            assertTrue(Solver(dims).isLegal(solved), "grid ${dims.size} broke a rule")
        }
    }

    @Test
    fun `the same seed fills the grid the same way`() {
        val first = Solver(Dimensions.CLASSIC).solve(Board(Dimensions.CLASSIC), Random(42))
        val second = Solver(Dimensions.CLASSIC).solve(Board(Dimensions.CLASSIC), Random(42))
        assertEquals(first, second)
    }

    private companion object {
        const val EASY =
            "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
        const val EASY_SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

        /** A 17 clue puzzle. 17 is the proven minimum for a unique solution. */
        const val HARD_17_CLUE =
            "000000010400000000020000000000050407008000300001090000300400200050100000000806000"

        /**
         * Legal, but the top left cell is dead: its row already holds 2 to 9, and its
         * column already holds the 1.
         */
        const val UNSOLVABLE =
            ".23456789....................................1..................................."
    }
}
