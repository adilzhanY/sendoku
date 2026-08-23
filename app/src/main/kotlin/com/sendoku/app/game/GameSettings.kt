package com.sendoku.app.game

import androidx.compose.runtime.Immutable

/**
 * The choices a player can make about how the board helps them.
 *
 * Every one of these defaults to on except the mistake limit. Sendoku is not trying to make
 * the game harder by withholding conveniences: the difficulty is in the puzzle, and it is
 * already past what most apps ship. Someone who wants a bare board can turn these off, and
 * the ones who do are exactly the ones who will find the settings screen.
 */
@Immutable
public data class GameSettings(
    /** Shade the row, column and box of the selected cell. */
    val highlightPeers: Boolean = true,

    /** Shade every other cell holding the same digit as the selected one. */
    val highlightSameDigit: Boolean = true,

    /** Rub out a pencil mark when the digit it names is placed where it can see it. */
    val autoClearMarks: Boolean = true,

    /** Mark a digit that repeats in a row, column or box. */
    val flagConflicts: Boolean = true,

    /** Show the clock. Off is a real mode, not a novelty: some people play to think. */
    val showTimer: Boolean = true,

    /**
     * How many wrong digits end the game, or null for no limit.
     *
     * Null is the default on purpose. A limit turns a puzzle into a test, and the hardest
     * grades here take long enough that losing an hour to a slip would be miserable.
     */
    val mistakeLimit: Int? = null,
) {
    init {
        require(mistakeLimit == null || mistakeLimit > 0) {
            "a mistake limit of $mistakeLimit ends the game before it starts"
        }
    }
}
