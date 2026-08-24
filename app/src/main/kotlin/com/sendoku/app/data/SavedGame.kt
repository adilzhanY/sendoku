package com.sendoku.app.data

import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.engine.Board
import com.sendoku.engine.Candidates
import com.sendoku.engine.Digits
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A game in progress, flattened into things a database column can hold.
 *
 * The puzzle is stored whole rather than as an index into the shipped batch. An index is
 * four bytes and would be wrong the first time the batch is regenerated, silently handing
 * the player back a different puzzle than the one they left. A hundred and sixty two
 * characters is nothing next to getting that wrong.
 *
 * The undo history is deliberately not saved. Restoring it would mean serialising every
 * cell a move touched, for every move, and a player coming back to a puzzle tomorrow is not
 * expecting to undo their way back into yesterday. The board comes back exactly as they
 * left it, which is the part that matters.
 */
public data class SavedGame(
    val givens: String,
    val solution: String,
    val entries: String,
    val marks: String,
    val grade: Grade,
    val rating: Double,
    val hardest: TechniqueId?,
    val selected: Int?,
    val pencilMode: Boolean,
    val elapsed: Duration,
    val mistakes: Int,
    val hintsUsed: Int,
    val dailyEpochDay: Long? = null,
) {

    /**
     * How many cells the player has filled in.
     *
     * The clues do not count. A puzzle opened and closed again is nought per cent done, and
     * counting the given digits made an untouched Gentle grid report itself as two fifths
     * finished, which is both wrong and dispiriting in the same breath.
     */
    public val placed: Int
        get() = entries.count { it != EMPTY_CHAR }

    /** How many cells there were to fill, which is the blanks rather than the whole grid. */
    public val total: Int get() = givens.count { it == EMPTY_CHAR }

    /** Rebuilds a playable game. */
    public fun toState(settings: GameSettings): GameState {
        val dims = dimensionsFor(givens.length)
        val givenBoard = Board.parse(dims, givens)
        val solutionBoard = Board.parse(dims, solution)
        val entered = Board.parse(dims, entries)
        val pencilled = decodeMarks(marks, dims.cellCount)

        return GameState(
            dims = dims,
            solution = solutionBoard,
            grade = grade,
            rating = rating,
            hardest = hardest,
            cells = (0 until dims.cellCount).map { index ->
                val given = givenBoard.atIndex(index)
                if (given != Board.EMPTY) {
                    Cell(digit = given, isGiven = true)
                } else {
                    Cell(digit = entered.atIndex(index), marks = pencilled[index])
                }
            },
            selected = selected,
            pencilMode = pencilMode,
            elapsed = elapsed,
            mistakes = mistakes,
            hintsUsed = hintsUsed,
            dailyEpochDay = dailyEpochDay,
            settings = settings,
        )
    }

    public companion object {

        /** Flattens a game so it can be written down. */
        public fun of(state: GameState): SavedGame = SavedGame(
            givens = buildString {
                for (cell in state.cells) {
                    append(Digits.toChar(if (cell.isGiven) cell.digit else Board.EMPTY))
                }
            },
            solution = state.solution.toString().replace("\n", ""),
            entries = buildString {
                for (cell in state.cells) {
                    append(Digits.toChar(if (cell.isGiven) Board.EMPTY else cell.digit))
                }
            },
            marks = encodeMarks(state.cells.map { it.marks }),
            grade = state.grade,
            rating = state.rating,
            hardest = state.hardest,
            selected = state.selected,
            pencilMode = state.pencilMode,
            elapsed = state.elapsed,
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            dailyEpochDay = state.dailyEpochDay,
        )

        /**
         * Pencil marks, one cell per character.
         *
         * A mark set is a bitmask of at most nine bits, which fits in a single base thirty
         * two digit twice over. Writing it as one character per cell keeps the column the
         * same width as the board itself and makes a corrupt value obvious on sight.
         */
        internal fun encodeMarks(marks: List<Candidates>): String = buildString {
            for (set in marks) {
                require(set.mask in 0..0xFFF) { "a mark set of ${set.mask} does not fit" }
                append(set.mask.toString(RADIX).padStart(MARK_WIDTH, '0'))
            }
        }

        internal fun decodeMarks(encoded: String, cellCount: Int): List<Candidates> {
            require(encoded.length == cellCount * MARK_WIDTH) {
                "pencil marks are ${encoded.length} characters, expected ${cellCount * MARK_WIDTH}"
            }
            return (0 until cellCount).map { index ->
                val from = index * MARK_WIDTH
                Candidates(encoded.substring(from, from + MARK_WIDTH).toInt(RADIX))
            }
        }

        /** The only shapes the app ships. A saved game of another size is not ours. */
        internal fun dimensionsFor(cellCount: Int): Dimensions = when (cellCount) {
            Dimensions.CLASSIC.cellCount -> Dimensions.CLASSIC
            Dimensions.SIX.cellCount -> Dimensions.SIX
            Dimensions.JUNIOR.cellCount -> Dimensions.JUNIOR
            Dimensions.HEXADOKU.cellCount -> Dimensions.HEXADOKU
            else -> error("a saved game of $cellCount cells is not a shape this app knows")
        }

        private const val RADIX = 32
        private const val MARK_WIDTH = 3
        private const val EMPTY_CHAR = '.'
    }
}

/** A game that is over, and what it cost. */
public data class FinishedGame(
    val givens: String,
    val grade: Grade,
    val rating: Double,
    val hardest: TechniqueId?,
    val elapsed: Duration,
    val hintsUsed: Int,
    val mistakes: Int,
    val solved: Boolean,
    val finishedAt: Long,
    val dailyEpochDay: Long? = null,
) {
    public companion object {
        public fun of(state: GameState, finishedAt: Long): FinishedGame = FinishedGame(
            givens = buildString {
                for (cell in state.cells) {
                    append(Digits.toChar(if (cell.isGiven) cell.digit else Board.EMPTY))
                }
            },
            grade = state.grade,
            rating = state.rating,
            hardest = state.hardest,
            elapsed = state.elapsed,
            hintsUsed = state.hintsUsed,
            mistakes = state.mistakes,
            solved = state.isSolved,
            finishedAt = finishedAt,
            dailyEpochDay = state.dailyEpochDay,
        )
    }
}

/** Seconds, for a column. Duration is a Kotlin idea and SQLite has never heard of it. */
internal fun Duration.toSeconds(): Long = inWholeSeconds

internal fun Long.toDuration(): Duration = this.seconds
