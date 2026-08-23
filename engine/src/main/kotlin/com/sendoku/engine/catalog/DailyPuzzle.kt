package com.sendoku.engine.catalog

import com.sendoku.engine.Grade

/**
 * Which puzzle is today's, worked out from the date alone.
 *
 * There is no server, and there is never going to be one. Everybody on a given version of
 * the app opens the same puzzle on the same day because the date picks it, not because
 * something told them what it was. That is the whole trick, and it is why the daily puzzle
 * costs nothing to run.
 *
 * The choice is made over the batch that ships in the app rather than by generating from a
 * seed. Generating would also be deterministic, but a Beyond puzzle takes thousands of
 * attempts to find, and nobody should wait for that to see today's grid.
 */
public object DailyPuzzle {

    /**
     * The week, Monday first.
     *
     * Borrowed from the newspapers, which have had decades to work out that a hard puzzle
     * on a Monday morning loses you a reader. It ramps: an easy start, a real climb by
     * Friday, and the two hardest grades saved for the days people have time for them.
     */
    public val rotation: List<Grade> = listOf(
        Grade.GENTLE,
        Grade.STEADY,
        Grade.TRICKY,
        Grade.SEVERE,
        Grade.SEVERE,
        Grade.DIABOLICAL,
        Grade.BEYOND,
    )

    /** Monday is zero. Epoch day zero was a Thursday, which is where the three comes from. */
    public fun weekdayOf(epochDay: Long): Int = Math.floorMod(epochDay + 3L, 7L).toInt()

    public fun gradeFor(epochDay: Long): Grade = rotation[weekdayOf(epochDay)]

    /**
     * Which puzzle of that grade, out of [choices].
     *
     * Walks the batch by a stride that shares no factor with its size, which means every
     * puzzle comes up once before any comes up twice. With five hundred of each grade and
     * one or two days a week apiece, that is the best part of a decade before a repeat.
     *
     * Hashing the date instead would have been simpler and much worse. A hash scatters, and
     * scattering means collisions: three hundred and sixty five draws from five hundred by
     * hash lands on only about two hundred and sixty distinct puzzles, so a player would
     * start meeting the same grid again within months.
     */
    public fun indexFor(epochDay: Long, choices: Int): Int {
        require(choices > 0) { "there are no puzzles to choose from" }
        val week = Math.floorDiv(epochDay, 7L)
        val stride = strideFor(choices)
        // The weekday is added so that a grade appearing twice in one week gets two puzzles.
        return Math.floorMod(week * stride + weekdayOf(epochDay), choices.toLong()).toInt()
    }

    /**
     * A step size coprime with [choices], so stepping visits every index before repeating.
     *
     * Any coprime stride would do. This one starts near two fifths of the way along, which
     * keeps consecutive weeks far apart in the batch rather than adjacent.
     */
    internal fun strideFor(choices: Int): Long {
        if (choices <= 2) return 1L
        var stride = (choices * 2L / 5L).coerceAtLeast(2L)
        while (gcd(stride, choices.toLong()) != 1L) stride++
        return stride
    }

    private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
}
