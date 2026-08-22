package com.sendoku.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoardTest {

    @Test
    fun `parses a single line and prints it back as rows`() {
        val text = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
        val board = Board.parse(Dimensions.CLASSIC, text)
        assertEquals(30, board.clueCount)
        assertEquals(51, board.emptyCount)
        assertEquals(5, board[0, 0])
        assertEquals(9, board[8, 8])
        assertEquals(text, board.toString().replace("\n", ""))
    }

    @Test
    fun `boxes map the way a 6 by 6 grid is drawn`() {
        val six = Dimensions.SIX
        assertEquals(6, six.size)
        assertEquals(0, six.boxOf(0, 0))
        assertEquals(0, six.boxOf(1, 2))
        assertEquals(1, six.boxOf(0, 3))
        assertEquals(4, six.boxOf(4, 0))
        assertEquals(5, six.boxOf(5, 5))
    }

    @Test
    fun `reads and writes hexadoku digits as letters`() {
        val board = Board(Dimensions.HEXADOKU)
        board[0, 0] = 16
        assertEquals('G', Digits.toChar(16))
        assertEquals(16, Digits.fromChar('g', 16))
        assertTrue(board.toString().startsWith("G"))
    }

    @Test
    fun `rejects a digit that is too large for the grid`() {
        val board = Board(Dimensions.JUNIOR)
        assertFailsWith<IllegalArgumentException> { board[0, 0] = 5 }
    }

    @Test
    fun `rejects text of the wrong length`() {
        assertFailsWith<IllegalArgumentException> { Board.parse(Dimensions.CLASSIC, "123") }
    }
}
