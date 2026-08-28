package com.sendoku.app.data

import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Placement
import com.sendoku.app.game.PuzzleOrigin
import com.sendoku.engine.Board
import com.sendoku.engine.Candidates
import com.sendoku.engine.Digits
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.killer.Cage
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
    /** Where the puzzle came from, so a shared or entered game does not open a level. */
    val origin: PuzzleOrigin = PuzzleOrigin.LADDER,
    /** Its place in the shipped batch, when it came from there, so it can be shared short. */
    val catalogIndex: Int? = null,
    /**
     * Cells the player has tinted, written as one character per cell.
     *
     * A dot for no tint and a digit for which one, so it reads like the board next to it and
     * a corrupt value is obvious on sight. Empty when nothing has been coloured, which is
     * every game anybody played before this existed.
     */
    val tints: String = "",
    /**
     * Every digit placed and when, as `cell.digit.seconds` triples.
     *
     * Written down because the post mortem is about a whole solve and a solve can be put
     * down and picked up tomorrow. Fifty or so triples is a few hundred characters, which is
     * nothing next to being unable to say anything about a game that took two sittings.
     */
    val placements: String = "",
    /** Whether a note was ever written on this board. */
    val notesUsed: Boolean = false,
    /**
     * The cages, when this is a Killer, as one character per cell naming its cage.
     *
     * Sums are not stored: a cage's sum is its cells added up in the solution, which is
     * already here, so writing it as well would let a damaged row disagree with itself.
     * Empty for an ordinary puzzle, which is every puzzle before Killer existed.
     */
    val cages: String = "",
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
            origin = origin,
            catalogIndex = catalogIndex,
            tints = decodeTints(tints, dims.cellCount),
            placements = decodePlacements(placements),
            notesUsed = notesUsed,
            cages = decodeCages(cages, solutionBoard),
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
            origin = state.origin,
            catalogIndex = state.catalogIndex,
            tints = encodeTints(state.tints, state.dims.cellCount),
            placements = encodePlacements(state.placements),
            notesUsed = state.notesUsed,
            cages = encodeCages(state.cages, state.dims.cellCount),
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

        /**
         * One character per cell naming its cage, in base thirty six.
         *
         * A grid has at most eighty one cages and a digit and a letter between them cover
         * thirty six, so a cage index above thirty five is written as the last character
         * rather than wrapping: at that point the layout is one cage per cell and there is
         * nothing to draw anyway. Every layout this app generates has under forty.
         */
        internal fun encodeCages(cages: List<Cage>, cellCount: Int): String {
            if (cages.isEmpty()) return ""
            val owner = IntArray(cellCount)
            for ((index, cage) in cages.withIndex()) {
                for (cell in cage.cells) owner[cell] = index
            }
            return buildString { for (cell in 0 until cellCount) append(owner[cell].digitToChar(CAGE_RADIX)) }
        }

        /** The other way round, with each cage's sum added up out of the solution. */
        internal fun decodeCages(encoded: String, solution: Board): List<Cage> {
            if (encoded.isEmpty()) return emptyList()
            val members = LinkedHashMap<Int, MutableList<Int>>()
            for ((cell, char) in encoded.withIndex()) {
                val index = runCatching { char.digitToInt(CAGE_RADIX) }.getOrNull() ?: return emptyList()
                members.getOrPut(index) { ArrayList() }.add(cell)
            }
            return members.entries.sortedBy { it.key }.map { (_, cells) ->
                Cage(cells.sumOf { solution.atIndex(it) }, cells.sorted())
            }
        }

        /** Cell, digit and clock reading, three numbers a piece, separated by full stops. */
        internal fun encodePlacements(placements: List<Placement>): String =
            placements.joinToString(SEPARATOR) { "${it.cell}.${it.digit}.${it.at}" }

        /** The other way round, dropping anything that does not read as three numbers. */
        internal fun decodePlacements(encoded: String): List<Placement> {
            if (encoded.isEmpty()) return emptyList()
            return encoded.split(SEPARATOR).mapNotNull { entry ->
                val parts = entry.split('.')
                if (parts.size != PLACEMENT_PARTS) return@mapNotNull null
                val cell = parts[0].toIntOrNull() ?: return@mapNotNull null
                val digit = parts[1].toIntOrNull() ?: return@mapNotNull null
                val at = parts[2].toIntOrNull() ?: return@mapNotNull null
                Placement(cell, digit, at)
            }
        }

        /** One character per cell: a dot for no tint, otherwise which one. */
        internal fun encodeTints(tints: Map<Int, Int>, cellCount: Int): String {
            if (tints.isEmpty()) return ""
            return buildString {
                for (index in 0 until cellCount) {
                    val tint = tints[index]
                    append(if (tint == null) EMPTY_CHAR else ('0' + tint))
                }
            }
        }

        /**
         * The other way round, forgiving anything it does not recognise.
         *
         * A saved game from a build with more tints than this one comes back with the ones
         * it knows and drops the rest, which is a colour missing rather than a crash.
         */
        internal fun decodeTints(encoded: String, cellCount: Int): Map<Int, Int> {
            if (encoded.length != cellCount) return emptyMap()
            val tints = HashMap<Int, Int>()
            for (index in 0 until cellCount) {
                val char = encoded[index]
                if (char in '0'..'9') tints[index] = char - '0'
            }
            return tints
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
        private const val SEPARATOR = ","
        private const val PLACEMENT_PARTS = 3
        private const val CAGE_RADIX = 36
    }
}

/** A game that is over, and what it cost. */
public data class FinishedGame(
    val givens: String,
    /** Every digit on the board when it ended, the player's and the puzzle's alike. */
    val board: String? = null,
    val grade: Grade,
    val rating: Double,
    val hardest: TechniqueId?,
    val elapsed: Duration,
    val hintsUsed: Int,
    val mistakes: Int,
    val solved: Boolean,
    val finishedAt: Long,
    val dailyEpochDay: Long? = null,
    /** Where the puzzle came from. Only a ladder or a daily win opens the level above. */
    val origin: PuzzleOrigin = PuzzleOrigin.LADDER,
    /** Its place in the shipped batch, when it came from there, so it can be shared short. */
    val catalogIndex: Int? = null,
    /**
     * Whether a note was ever written while solving this.
     *
     * The third of the three facts a clean solve is made of. The other two, no hints and no
     * mistakes, were already recorded because they are how a game can be lost.
     */
    val notesUsed: Boolean = false,
) {

    /** Solved with no hints, no mistakes and no notes written. Nothing else counts. */
    public val isClean: Boolean get() = solved && hintsUsed == 0 && mistakes == 0 && !notesUsed

    /**
     * The game as it ended, rebuilt well enough to look at and to share.
     *
     * Three cases, and only one of them costs anything. A game recorded since the board
     * column exists is read straight back. A won game recorded before it is rebuilt by
     * solving the givens again, which is the same grid it ended on because a won board is the
     * solution. A lost game recorded before it has no board anywhere, and this returns null
     * rather than drawing a grid the player never played.
     */
    public fun replay(settings: GameSettings = GameSettings()): GameState? {
        val dims = SavedGame.dimensionsFor(givens.length)
        val givenBoard = Board.parse(dims, givens)
        val solved = Solver(dims).solve(givenBoard) ?: return null
        val ended = board?.let { Board.parse(dims, it) } ?: solved.takeIf { this.solved } ?: return null
        return GameState(
            dims = dims,
            solution = solved,
            grade = grade,
            rating = rating,
            hardest = hardest,
            cells = (0 until dims.cellCount).map { index ->
                val given = givenBoard.atIndex(index)
                if (given != Board.EMPTY) {
                    Cell(digit = given, isGiven = true)
                } else {
                    Cell(digit = ended.atIndex(index))
                }
            },
            selected = null,
            pencilMode = false,
            elapsed = elapsed,
            mistakes = mistakes,
            hintsUsed = hintsUsed,
            dailyEpochDay = dailyEpochDay,
            origin = origin,
            catalogIndex = catalogIndex,
            settings = settings,
        )
    }

    public companion object {
        public fun of(state: GameState, finishedAt: Long): FinishedGame = FinishedGame(
            givens = buildString {
                for (cell in state.cells) {
                    append(Digits.toChar(if (cell.isGiven) cell.digit else Board.EMPTY))
                }
            },
            board = buildString { for (cell in state.cells) append(Digits.toChar(cell.digit)) },
            grade = state.grade,
            rating = state.rating,
            hardest = state.hardest,
            elapsed = state.elapsed,
            hintsUsed = state.hintsUsed,
            mistakes = state.mistakes,
            solved = state.isSolved,
            finishedAt = finishedAt,
            dailyEpochDay = state.dailyEpochDay,
            origin = state.origin,
            catalogIndex = state.catalogIndex,
            notesUsed = state.notesUsed,
        )
    }
}

/** Seconds, for a column. Duration is a Kotlin idea and SQLite has never heard of it. */
internal fun Duration.toSeconds(): Long = inWholeSeconds

internal fun Long.toDuration(): Duration = this.seconds
