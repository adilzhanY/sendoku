package com.sendoku.app.ui

import android.content.Context
import android.content.Intent
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.game.GameState
import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.catalog.PuzzleCode

/**
 * The code for a game, and sending it to somebody.
 *
 * Two kinds, chosen by what the app knows. A puzzle dealt out of the shipped batch knows
 * where it sits in it, so it can be named in five characters. Anything else, which means a
 * puzzle made on the phone once the batch ran out or one that arrived as a grid in the first
 * place, is written out in full.
 *
 * Sent as text rather than as the picture. The card is the boast and the code is the puzzle,
 * and somebody who wants to play the same grid needs the second one.
 */
internal object ShareCode {

    fun of(state: GameState): String = state.catalogIndex
        ?.let { PuzzleCode.forBatch(it) }
        ?: PuzzleCode.forGrid(givensOf(state))

    fun of(game: FinishedGame): String = game.catalogIndex
        ?.let { PuzzleCode.forBatch(it) }
        ?: PuzzleCode.forGrid(Board.parse(dimensionsFor(game.givens.length), game.givens))

    /**
     * Hands the code to whatever the player shares with.
     *
     * The link goes with it. A code on its own is something to paste into the app; a link is
     * something to tap, and the same thing reads as both.
     */
    fun send(context: Context, code: String, invitation: String, chooser: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$invitation\n\n$code\n$LINK$code")
        }
        context.startActivity(Intent.createChooser(intent, chooser))
    }

    private fun givensOf(state: GameState): Board {
        val board = Board(state.dims)
        for ((index, cell) in state.cells.withIndex()) {
            if (cell.isGiven) board.setAtIndex(index, cell.digit)
        }
        return board
    }

    private fun dimensionsFor(cellCount: Int): Dimensions = when (cellCount) {
        Dimensions.JUNIOR.cellCount -> Dimensions.JUNIOR
        Dimensions.SIX.cellCount -> Dimensions.SIX
        Dimensions.HEXADOKU.cellCount -> Dimensions.HEXADOKU
        else -> Dimensions.CLASSIC
    }

    const val LINK: String = "sendoku://p/"
}
