package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("slow")
class GridEquivalenceTest {

    private val classic = Dimensions.CLASSIC

    private fun puzzle(seed: Long) = Generator(classic, Random(seed)).generate().givens

    private fun relabel(board: Board, shift: Int): Board {
        val out = Board(classic)
        for (index in 0 until 81) {
            val digit = board.atIndex(index)
            if (digit != Board.EMPTY) out.setAtIndex(index, (digit - 1 + shift) % 9 + 1)
        }
        return out
    }

    private fun reorderRows(board: Board, order: IntArray): Board {
        val out = Board(classic)
        for (row in 0 until 9) {
            for (col in 0 until 9) out[row, col] = board[order[row], col]
        }
        return out
    }

    private fun transpose(board: Board): Board {
        val out = Board(classic)
        for (row in 0 until 9) {
            for (col in 0 until 9) out[col, row] = board[row, col]
        }
        return out
    }

    @Test
    fun `a puzzle is equivalent to itself`() {
        val board = puzzle(401)
        assertTrue(GridEquivalence.areEquivalent(board, board))
        assertEquals(GridEquivalence.fingerprint(board), GridEquivalence.fingerprint(board))
    }

    @Test
    fun `relabelling the digits changes nothing`() {
        val board = puzzle(403)
        for (shift in 1..8) {
            val disguised = relabel(board, shift)
            assertEquals(GridEquivalence.fingerprint(board), GridEquivalence.fingerprint(disguised))
            assertTrue(GridEquivalence.areEquivalent(board, disguised), "shift $shift slipped past")
        }
    }

    @Test
    fun `swapping two rows inside a band changes nothing`() {
        val board = puzzle(405)
        val disguised = reorderRows(board, intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8))
        assertEquals(GridEquivalence.fingerprint(board), GridEquivalence.fingerprint(disguised))
        assertTrue(GridEquivalence.areEquivalent(board, disguised))
    }

    @Test
    fun `swapping whole bands changes nothing`() {
        val board = puzzle(407)
        val disguised = reorderRows(board, intArrayOf(6, 7, 8, 3, 4, 5, 0, 1, 2))
        assertTrue(GridEquivalence.areEquivalent(board, disguised))
    }

    @Test
    fun `reflecting across the diagonal changes nothing`() {
        val board = puzzle(409)
        val disguised = transpose(board)
        assertEquals(GridEquivalence.fingerprint(board), GridEquivalence.fingerprint(disguised))
        assertTrue(GridEquivalence.areEquivalent(board, disguised))
    }

    @Test
    fun `a disguise built from every trick at once is still caught`() {
        val board = puzzle(411)
        val disguised = relabel(
            transpose(reorderRows(board, intArrayOf(2, 0, 1, 8, 6, 7, 4, 5, 3))),
            5,
        )
        assertEquals(GridEquivalence.fingerprint(board), GridEquivalence.fingerprint(disguised))
        assertTrue(GridEquivalence.areEquivalent(board, disguised))
    }

    @Test
    fun `moving a row into another band is not a disguise, it is a different puzzle`() {
        val board = puzzle(413)
        val illegal = reorderRows(board, intArrayOf(3, 1, 2, 0, 4, 5, 6, 7, 8))
        assertFalse(GridEquivalence.areEquivalent(board, illegal))
    }

    @Test
    fun `two unrelated puzzles are not equivalent`() {
        val first = puzzle(415)
        val second = puzzle(417)
        assertFalse(GridEquivalence.areEquivalent(first, second))
    }

    @Test
    fun `a fingerprint match does not by itself mean equivalent`() {
        // The fingerprint is a filter, not a verdict, and the test suite should say so.
        var collisions = 0
        val seen = HashMap<String, Board>()
        for (seed in 500L until 700L) {
            val board = puzzle(seed)
            val print = GridEquivalence.fingerprint(board)
            val other = seen.put(print, board) ?: continue
            collisions++
            assertFalse(
                GridEquivalence.areEquivalent(board, other),
                "seed $seed really is a duplicate",
            )
        }
        println("FINGERPRINT $collisions collisions in 200 puzzles, none of them real")
    }

    @Test
    fun `a puzzle with a different clue count is rejected at once`() {
        val board = puzzle(419)
        val thinner = board.copy()
        thinner.setAtIndex((0 until 81).first { thinner.atIndex(it) != Board.EMPTY }, Board.EMPTY)
        assertFalse(GridEquivalence.areEquivalent(board, thinner))
    }
}
