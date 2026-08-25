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

    /**
     * Keep every empty cell pencilled with the digits it could still take.
     *
     * Off by default, and the default is the interesting decision. Doing the bookkeeping by
     * hand is most of what makes an easy puzzle take time, and skipping it is how most
     * people play on a phone. But the whole grid covered in small digits is also how a
     * beginner learns to stop looking at the board and start reading their own notes, and
     * the notes are only right because the app wrote them.
     *
     * So it is offered, plainly, and not chosen for anybody. Anything above Expert is
     * unplayable without it, and a player who reaches those levels will find it.
     */
    val autoNotes: Boolean = false,

    /**
     * Mark a digit that is not the one the answer wants, the moment it goes in.
     *
     * Different from [flagConflicts], which only catches a digit that repeats in a house.
     * A wrong digit that breaks no rule yet is invisible until the puzzle falls apart
     * twenty moves later, and finding out then is miserable.
     *
     * Off by default because it changes the game: with it on there is nothing to lose by
     * trying a digit, and a puzzle you cannot get wrong is a puzzle with no tension in it.
     * The settings screen says so rather than leaving somebody to discover it.
     */
    val autoCheck: Boolean = false,

    /**
     * Shade every cell that could still take the selected digit.
     *
     * The scanning aid. Selecting a 7 lights up everywhere a 7 could still go, which is the
     * question a player is asking when they pick up a digit in the first place. Off by
     * default: it is the strongest of these and it does a real part of the work.
     */
    val highlightHomes: Boolean = false,

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

    /**
     * How much a hint says when it opens.
     *
     * Somebody working through the course wants the whole argument and should not have to
     * tap three times for it every time. Somebody who only ever wants a nudge should not
     * have to look away quickly to avoid being told the answer. Neither of them is the
     * default player, so the default is the middle: the name of the technique.
     */
    val hintDetail: HintLevel = HintLevel.NAME,

    /** A small buzz when a digit goes in. On, because a board gives no other feedback. */
    val haptics: Boolean = true,

    /**
     * The sounds. On, and quiet.
     *
     * They were off, on the argument that a puzzle app which makes noise in a quiet room is
     * one that gets uninstalled. That argument was against the noise, not against sound: the
     * set here is soft, short and tuned so that no two of them clash, and a board that
     * answers when you touch it is a great deal nicer to play than one that says nothing.
     * Off is one switch away, and the phone's own silent mode still wins over both.
     */
    val sound: Boolean = true,
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
