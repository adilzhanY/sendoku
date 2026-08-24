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
    val mistakeLimit: Int? = 3,

    /**
     * How many hints end the game, or null for as many as you like.
     *
     * A limit rather than a paywall. Nothing here is sold and nothing is held back behind an
     * advertisement: three are given freely and the fourth is the one that costs the game,
     * which makes a hint a decision rather than a reflex.
     *
     * Turn it off in settings and hints are unlimited again, which is the right setting for
     * somebody working through the course.
     */
    val hintLimit: Int? = 3,

    /** A small buzz when a digit goes in. On, because a board gives no other feedback. */
    val haptics: Boolean = true,

    /**
     * A click when a digit goes in. Off, because a puzzle app that makes noise in a quiet
     * room is a puzzle app that gets uninstalled.
     */
    val sound: Boolean = false,
) {
    init {
        require(mistakeLimit == null || mistakeLimit > 0) {
            "a mistake limit of $mistakeLimit ends the game before it starts"
        }
        require(hintLimit == null || hintLimit > 0) {
            "a hint limit of $hintLimit ends the game before it starts"
        }
    }
}
