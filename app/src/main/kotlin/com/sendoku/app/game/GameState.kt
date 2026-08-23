package com.sendoku.app.game

import androidx.compose.runtime.Immutable
import com.sendoku.engine.Board
import com.sendoku.engine.Candidates
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Geometry
import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId
import kotlin.time.Duration

/** One square of the board, as the player has left it. */
@Immutable
public data class Cell(
    val digit: Int = Board.EMPTY,
    val marks: Candidates = Candidates.EMPTY,
    /** A clue the puzzle came with. Never editable, and never wrong. */
    val isGiven: Boolean = false,
) {
    val isEmpty: Boolean get() = digit == Board.EMPTY
}

/** What kind of change a move was, for labelling an undo. */
public enum class MoveKind {
    PLACE,
    MARK,
    ERASE,
}

/**
 * One undoable change, stored as the cells before and after it.
 *
 * Recording whole cells rather than a description of the edit is what makes undo boring,
 * and undo should be boring. Placing a digit can also rub out pencil marks in up to twenty
 * other cells, and a move that has to explain how to reverse that is a move that will
 * eventually get it wrong.
 */
@Immutable
public data class Move(val kind: MoveKind, val cell: Int, val before: Map<Int, Cell>, val after: Map<Int, Cell>)

/**
 * Everything about a game in progress.
 *
 * Immutable, and pure Kotlin with no Android in it. Every action returns a new state, which
 * means undo is a list of what the cells used to be rather than a pile of special cases,
 * and it means the whole of the game's behaviour can be tested without an emulator.
 *
 * The solution is held here, which sounds like cheating and is not. It is needed to count
 * mistakes and it is needed for hints, and it reaches the screen only through those two,
 * both of which the player asked for.
 */
@Immutable
public data class GameState(
    val dims: Dimensions,
    val solution: Board,
    val grade: Grade,
    val rating: Double,
    /** The hardest rule the puzzle was rated as needing. What the win screen reports. */
    val hardest: TechniqueId?,
    val cells: List<Cell>,
    val selected: Int? = null,
    val pencilMode: Boolean = false,
    val elapsed: Duration = Duration.ZERO,
    val isRunning: Boolean = true,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val past: List<Move> = emptyList(),
    val future: List<Move> = emptyList(),
    val settings: GameSettings = GameSettings(),
) {

    private val geometry: Geometry get() = Geometry.of(dims)

    public val size: Int get() = dims.size

    public val cellCount: Int get() = dims.cellCount

    /** True when every square holds the digit the solution wants there. */
    public val isSolved: Boolean
        get() = cells.indices.all { cells[it].digit == solution.atIndex(it) }

    /** True when the player has run out of allowed mistakes. */
    public val isFailed: Boolean
        get() = settings.mistakeLimit?.let { mistakes >= it } ?: false

    public val isOver: Boolean get() = isSolved || isFailed

    public val canUndo: Boolean get() = past.isNotEmpty()

    public val canRedo: Boolean get() = future.isNotEmpty()

    /**
     * Whether erase would do anything.
     *
     * A lit button that does nothing when pressed reads as a broken app, and this one had
     * two ways to look lit and do nothing: an empty selected cell, and a given.
     */
    public val canErase: Boolean
        get() {
            val at = selected ?: return false
            if (isOver) return false
            val cell = cells[at]
            return !cell.isGiven && (!cell.isEmpty || cell.marks.isNotEmpty)
        }

    /** How many of [digit] are still to be placed. */
    public fun remaining(digit: Int): Int = size - cells.count { it.digit == digit }

    /** True when every one of [digit] is on the board, so the pad key has nothing left to do. */
    public fun isExhausted(digit: Int): Boolean = remaining(digit) <= 0

    /**
     * Cells holding a digit that already appears elsewhere in the same row, column or box.
     *
     * This is about the grid, not about the answer. A digit can be wrong without repeating
     * anything, and that is not a conflict, it is a mistake. The two are counted separately
     * because they tell the player different things.
     */
    public val conflicts: Set<Int>
        get() {
            if (!settings.flagConflicts) return emptySet()
            val clashing = HashSet<Int>()
            for (cell in cells.indices) {
                val digit = cells[cell].digit
                if (digit == Board.EMPTY) continue
                for (peer in geometry.peersOf(cell)) {
                    if (cells[peer].digit == digit) {
                        clashing.add(cell)
                        clashing.add(peer)
                    }
                }
            }
            return clashing
        }

    /** Cells sharing a row, column or box with the selection, when the setting allows it. */
    public val highlightedPeers: Set<Int>
        get() {
            val at = selected ?: return emptySet()
            if (!settings.highlightPeers) return emptySet()
            return geometry.peersOf(at).toSet()
        }

    /** Other cells holding the same digit as the selection, when the setting allows it. */
    public val highlightedMatches: Set<Int>
        get() {
            val at = selected ?: return emptySet()
            if (!settings.highlightSameDigit) return emptySet()
            val digit = cells[at].digit
            if (digit == Board.EMPTY) return emptySet()
            return cells.indices.filter { it != at && cells[it].digit == digit }.toSet()
        }

    public fun select(cell: Int?): GameState {
        require(cell == null || cell in 0 until cellCount) { "cell $cell is off the grid" }
        return copy(selected = cell)
    }

    public fun setPencilMode(on: Boolean): GameState = copy(pencilMode = on)

    public fun togglePencilMode(): GameState = copy(pencilMode = !pencilMode)

    public fun withSettings(settings: GameSettings): GameState = copy(settings = settings)

    /**
     * Puts [digit] in the selected cell, or pencils it in when in pencil mode.
     *
     * Entering the digit that is already there rubs it out again, which is how every sudoku
     * app behaves and is what a player expects from tapping the same key twice.
     */
    public fun enter(digit: Int): GameState {
        require(digit in 1..size) { "$digit is not a digit on a $size by $size board" }
        val at = selected ?: return this
        val cell = cells[at]
        if (cell.isGiven || isOver) return this

        if (pencilMode) {
            if (!cell.isEmpty) return this
            val marks = if (digit in cell.marks) cell.marks - digit else cell.marks + digit
            return apply(MoveKind.MARK, at, mapOf(at to cell.copy(marks = marks)))
        }

        if (cell.digit == digit) {
            return apply(MoveKind.ERASE, at, mapOf(at to cell.copy(digit = Board.EMPTY)))
        }

        val changes = HashMap<Int, Cell>()
        changes[at] = Cell(digit = digit, marks = Candidates.EMPTY, isGiven = false)
        if (settings.autoClearMarks) {
            for (peer in geometry.peersOf(at)) {
                val other = cells[peer]
                if (digit in other.marks) changes[peer] = other.copy(marks = other.marks - digit)
            }
        }

        val wrong = digit != solution.atIndex(at)
        return apply(MoveKind.PLACE, at, changes)
            .copy(mistakes = mistakes + if (wrong) 1 else 0)
            .stopIfOver()
    }

    /**
     * Moves the selection by whole cells, wrapping at the edges.
     *
     * For a keyboard, which a tablet or a Chromebook will have. Wrapping rather than
     * stopping at the edge, because a player holding an arrow key wants to keep going and
     * a silent stop reads as a dropped keypress.
     */
    public fun moveSelection(rows: Int, columns: Int): GameState {
        val from = selected ?: return select(0)
        val row = Math.floorMod(from / size + rows, size)
        val column = Math.floorMod(from % size + columns, size)
        return select(row * size + column)
    }

    /**
     * The digits [cell] could still take, going by what its peers already hold.
     *
     * This is the player's own bookkeeping done for them, not a hint. It says nothing the
     * board is not already showing, which is why it can be offered without touching the
     * hint counter.
     */
    public fun candidatesAt(cell: Int): Candidates {
        if (!cells[cell].isEmpty) return Candidates.EMPTY
        var possible = Candidates.all(dims)
        for (peer in geometry.peersOf(cell)) {
            val digit = cells[peer].digit
            if (digit != Board.EMPTY) possible -= digit
        }
        return possible
    }

    /** Pencils in every digit the selected cell could still take. */
    public fun fillMarks(): GameState {
        val at = selected ?: return this
        val cell = cells[at]
        if (cell.isGiven || !cell.isEmpty || isOver) return this
        val possible = candidatesAt(at)
        if (possible == cell.marks) return this
        return apply(MoveKind.MARK, at, mapOf(at to cell.copy(marks = possible)))
    }

    /** Rubs out the pencil marks in the selected cell, leaving any digit alone. */
    public fun clearMarks(): GameState {
        val at = selected ?: return this
        val cell = cells[at]
        if (cell.marks.isEmpty || isOver) return this
        return apply(MoveKind.MARK, at, mapOf(at to cell.copy(marks = Candidates.EMPTY)))
    }

    /** True once the player has changed anything, so leaving would cost them something. */
    public val hasProgress: Boolean get() = past.isNotEmpty()

    /** Clears the digit and every pencil mark from the selected cell. */
    public fun erase(): GameState {
        val at = selected ?: return this
        val cell = cells[at]
        if (cell.isGiven || isOver) return this
        if (cell.isEmpty && cell.marks.isEmpty) return this
        return apply(MoveKind.ERASE, at, mapOf(at to Cell()))
    }

    /**
     * Clears several cells at once, as one undoable move.
     *
     * This is what the hint offers when it has found a wrong digit. Telling somebody their
     * board is broken and then leaving them to hunt for the cell is not a hint, it is a
     * riddle, and the undo history is gone the moment they close the app.
     */
    public fun eraseAll(targets: Set<Int>): GameState {
        if (isOver) return this
        val changes = targets
            .filter { it in cells.indices && !cells[it].isGiven && !cells[it].isEmpty }
            .associateWith { Cell() }
        if (changes.isEmpty()) return this
        return apply(MoveKind.ERASE, changes.keys.first(), changes).copy(selected = changes.keys.first())
    }

    public fun undo(): GameState {
        val move = past.lastOrNull() ?: return this
        return copy(
            cells = replaced(move.before),
            selected = move.cell,
            past = past.dropLast(1),
            future = future + move,
        )
    }

    public fun redo(): GameState {
        val move = future.lastOrNull() ?: return this
        return copy(
            cells = replaced(move.after),
            selected = move.cell,
            past = past + move,
            future = future.dropLast(1),
        )
    }

    /** Adds [delta] to the clock, when the clock is running and the game is not over. */
    public fun tick(delta: Duration): GameState = if (!isRunning || isOver) this else copy(elapsed = elapsed + delta)

    public fun pause(): GameState = copy(isRunning = false)

    public fun resume(): GameState = if (isOver) this else copy(isRunning = true)

    /**
     * Stops the clock the moment the game ends.
     *
     * Without this the timer keeps its running flag after the last digit goes in, and the
     * win screen shows a clock that looks like it is still counting. The elapsed time is
     * already frozen by [tick], but a player reads the state of the clock, not the code.
     */
    private fun stopIfOver(): GameState = if (isOver && isRunning) copy(isRunning = false) else this

    /**
     * Carries out a hint the player accepted.
     *
     * The struck pencil marks come out in one move and any placement follows in another, so
     * both can be undone. A hint that could not be taken back would be a trap for anyone who
     * tapped it by accident, and accepting help should never cost more than doing it by hand.
     */
    public fun applyHint(deduction: Deduction): GameState {
        val struck = HashMap<Int, Cell>()
        for ((cell, digit) in deduction.eliminations) {
            val current = struck[cell] ?: cells[cell]
            if (digit in current.marks) struck[cell] = current.copy(marks = current.marks - digit)
        }

        var next = if (struck.isEmpty()) this else apply(MoveKind.MARK, deduction.focusCells.firstOrNull() ?: 0, struck)
        for ((cell, digit) in deduction.placements) {
            next = next.select(cell).enter(digit)
        }
        return next
    }

    /** Records that the player asked for help. The hint itself belongs to the hint system. */
    public fun countHint(): GameState = copy(hintsUsed = hintsUsed + 1)

    /** The board as the engine sees it, for handing to the solver. */
    public fun toBoard(): Board {
        val board = Board(dims)
        for (index in cells.indices) board.setAtIndex(index, cells[index].digit)
        return board
    }

    private fun apply(kind: MoveKind, at: Int, changes: Map<Int, Cell>): GameState {
        val before = changes.keys.associateWith { cells[it] }
        if (before == changes) return this
        return copy(
            cells = replaced(changes),
            past = past + Move(kind, at, before, changes),
            // Doing something new throws away the branch you had undone your way out of.
            future = emptyList(),
        )
    }

    private fun replaced(changes: Map<Int, Cell>): List<Cell> {
        val next = cells.toMutableList()
        for ((index, cell) in changes) next[index] = cell
        return next
    }

    public companion object {

        /** Starts a game from a rated puzzle. */
        public fun start(rated: RatedPuzzle, settings: GameSettings = GameSettings()): GameState {
            val givens = rated.puzzle.givens
            val dims = givens.dims
            return GameState(
                dims = dims,
                solution = rated.puzzle.solution,
                grade = rated.grade,
                rating = rated.rating,
                hardest = rated.hardest,
                cells = (0 until dims.cellCount).map { index ->
                    val digit = givens.atIndex(index)
                    Cell(digit = digit, isGiven = digit != Board.EMPTY)
                },
                settings = settings,
            )
        }
    }
}
