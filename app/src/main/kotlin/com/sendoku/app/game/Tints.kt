package com.sendoku.app.game

/**
 * Tints [cell], or takes the tint off when it already has that one.
 *
 * Out here rather than on [GameState] because nothing in the game reads a tint. It does not
 * make a cell right or wrong, it does not count towards anything, and it is not undoable:
 * undo is for the board, and somebody who has just spent four taps colouring a chain does
 * not want it walking back through the colours instead of the digit they typed by mistake.
 *
 * What it is for is the thing the course teaches across three lessons and the board had no
 * way to do: pick a digit with two homes in a house, follow what each choice forces, and
 * look for the contradiction. Solvers do it on paper with two pencils.
 */
public fun GameState.tint(cell: Int, tint: Int): GameState {
    val next = tints.toMutableMap()
    if (next[cell] == tint) next.remove(cell) else next[cell] = tint
    return copy(tints = next)
}

/** Takes every tint off, which is the end of following one chain and the start of the next. */
public fun GameState.clearTints(): GameState = if (tints.isEmpty()) this else copy(tints = emptyMap())
