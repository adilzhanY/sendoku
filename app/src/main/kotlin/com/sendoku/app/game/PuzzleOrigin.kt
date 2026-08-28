package com.sendoku.app.game

/**
 * Where a puzzle came from, as far as the player's record is concerned.
 *
 * It exists because two of these must not count towards opening the next level. The levels
 * are earned by winning the one below, and a code pasted from a friend is not something the
 * player earned: somebody sent a Nightmare code to a beginner on their first day, and the
 * beginner should be able to play it and get nowhere near unlocking Nightmare.
 *
 * A puzzle typed in from a newspaper is the same case for the same reason, and it is here
 * now rather than later so that the column and its migration are written once.
 */
public enum class PuzzleOrigin {
    /** Dealt by the app at a level the player chose. The only kind that opens levels. */
    LADDER,

    /** Today's puzzle, or a day caught up on. Still earned, so it still counts. */
    DAILY,

    /** Arrived as a code from somebody else. */
    SHARED,

    /** Typed in by the player, from a newspaper or from anywhere else. */
    ENTERED,

    ;

    /** Whether winning this opens the level above it. */
    public val earnsProgress: Boolean get() = this == LADDER || this == DAILY

    public companion object {
        /**
         * Reads a stored name, forgiving anything it does not recognise.
         *
         * A row written by a later version naming an origin this build has never heard of
         * comes back as a ladder game, which is the safe wrong answer: it is what every row
         * written before this column existed is anyway.
         */
        public fun of(name: String?): PuzzleOrigin = entries.firstOrNull { it.name == name } ?: LADDER
    }
}
