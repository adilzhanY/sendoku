package com.sendoku.app.game

import com.sendoku.engine.Board
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.Techniques
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One place a solve slowed down, and what was on the board at the time.
 *
 * [spent] is how long the player sat there before the next digit landed. [available] is the
 * cheapest thing the ladder could have found on the board as it then stood, which is not an
 * accusation: it is what there was to see. Often it is something they did see, one move
 * later, which is exactly what a person wants confirmed.
 */
public data class Moment(val at: Duration, val spent: Duration, val available: TechniqueId)

/**
 * Where the time went, after a game that was won.
 *
 * Not a score, and deliberately not a grade. The app already knows how long the puzzle took
 * and what it needed; what it has never been able to say is where in the solve the time
 * actually went, which is the only part a player cannot reconstruct for themselves.
 *
 * The tone is the whole design. It says what was available at the moment somebody stopped,
 * never what they failed to see, and it stays quiet unless there is something worth saying.
 * A fast clean solve gets nothing at all, because an app that finds a lesson in every win is
 * an app that turns winning into being marked.
 */
public object PostMortem {

    /**
     * Long enough to be a pause rather than a breath.
     *
     * Ninety seconds on one cell is somebody looking, not somebody typing. Below that the
     * gaps are the ordinary rhythm of a solve and pointing at them would be noise.
     */
    private val WORTH_MENTIONING = 90.seconds

    /** How many moments to report. Two or three is a paragraph; more is a report card. */
    private const val MOST = 2

    /**
     * The two longest pauses in a solve, with what was available at each.
     *
     * Empty when the game was not won, when it was not played in a way that recorded its
     * placements, or when nothing in it took long enough to be worth mentioning. All three
     * of those mean the same thing to the caller: say nothing.
     */
    public fun of(state: GameState): List<Moment> {
        if (!state.isSolved || state.placements.isEmpty()) return emptyList()

        val board = Board(state.dims)
        for (index in state.cells.indices) {
            if (state.cells[index].isGiven) board.setAtIndex(index, state.cells[index].digit)
        }

        val moments = ArrayList<Moment>()
        var previous = 0
        for (placement in state.placements) {
            val gap = placement.at - previous
            if (gap >= WORTH_MENTIONING.inWholeSeconds) {
                // The board as it stood when the player stopped, which is before this digit
                // landed rather than after it.
                Techniques.availableOn(board)?.let { deduction ->
                    moments += Moment(
                        at = previous.seconds,
                        spent = gap.seconds,
                        available = deduction.technique,
                    )
                }
            }
            board.setAtIndex(placement.cell, placement.digit)
            previous = placement.at
        }
        return moments.sortedByDescending { it.spent }.take(MOST)
    }
}
