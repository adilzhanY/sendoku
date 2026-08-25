package com.sendoku.app.game

import com.sendoku.engine.Board
import com.sendoku.engine.Geometry
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind

/**
 * Why a digit cannot go in a cell.
 *
 * The question a beginner actually asks, and the one no sudoku app answers. Every app will
 * tell you what to do next; none will tell you why the thing you were about to do is wrong.
 * So a player tries a 4, gets told it is a mistake, and learns nothing except that they were
 * wrong, which they already knew.
 *
 * This answers from the board and nothing else. It never looks at the solution, which is
 * what makes it free: it says what a player could have seen by looking along a row, and
 * saying it out loud costs the puzzle nothing.
 *
 * There is one honest answer it has to be willing to give, and it is the interesting one:
 * "nothing rules it out". A digit can be wrong without being ruled out yet, and pretending
 * otherwise would make this a solution checker wearing a teacher's coat.
 */
public object WhyNot {

    /** What the board has to say about one digit in one cell. */
    public sealed interface Reason {

        /** A cell in the same house already holds the digit. */
        public data class Taken(val house: House, val by: Int) : Reason

        /** The cell already holds a digit, so the question is about that instead. */
        public data class Filled(val digit: Int) : Reason

        /** Nothing on the board rules it out. Which is not the same as it being right. */
        public data object Possible : Reason
    }

    /**
     * Asks the board about [digit] in [cell].
     *
     * When more than one house rules the digit out, the first one found is the answer. A
     * player needs one reason, not a catalogue, and the row is checked before the column
     * and the box because that is the order a person scans in.
     */
    public fun ask(state: GameState, cell: Int, digit: Int): Reason {
        require(digit in 1..state.size) { "$digit is not a digit on a ${state.size} by ${state.size} board" }
        val here = state.cells[cell].digit
        if (here != Board.EMPTY) return Reason.Filled(here)

        val geometry = Geometry.of(state.dims)
        val size = state.size
        val houses = listOf(
            House(HouseKind.ROW, cell / size),
            House(HouseKind.COLUMN, cell % size),
            House(HouseKind.BOX, state.dims.boxOf(cell / size, cell % size)),
        )
        for (house in houses) {
            val holder = geometry.cellsOf(house).firstOrNull { state.cells[it].digit == digit }
            if (holder != null) return Reason.Taken(house, holder)
        }
        return Reason.Possible
    }

    /** Every digit the board rules out of [cell], with the reason for each. */
    public fun all(state: GameState, cell: Int): Map<Int, Reason> =
        (1..state.size).associateWith { ask(state, cell, it) }
}
