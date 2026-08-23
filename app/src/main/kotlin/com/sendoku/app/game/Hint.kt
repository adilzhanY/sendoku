package com.sendoku.app.game

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.technique.CellDigit
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.Techniques

/**
 * How much of a hint the player has asked for.
 *
 * Three taps, three answers. The first says what kind of thing to look for, which is often
 * all somebody needs. The second says where. Only the third does the reasoning for them.
 *
 * Sudoku apps almost universally have one hint button that fills in a cell, which teaches
 * nothing and leaves the player no better at the next puzzle. This is the part of Sendoku
 * that is actually trying to make somebody better at sudoku.
 */
public enum class HintLevel {
    /** The name of the technique and the region it applies to. */
    NAME,

    /** The cells the argument rests on, lit up. */
    CELLS,

    /** The whole thing spelled out, with the option to apply it. */
    FULL,
    ;

    public val next: HintLevel get() = entries.getOrElse(ordinal + 1) { FULL }

    public val hasMore: Boolean get() = this != FULL
}

/** What the hint system has to say. */
public sealed interface Hint {

    /** A move that follows from the board, and the reasoning behind it. */
    public data class Step(val deduction: Deduction, val level: HintLevel) : Hint

    /**
     * A digit on the board is wrong.
     *
     * There is no point explaining the next deduction on a board that cannot be finished,
     * so this comes first and says where to look. It names the cells, not the right answers.
     */
    public data class Mistake(val cells: Set<Int>) : Hint

    /** Nothing left to say. */
    public data object Solved : Hint

    /**
     * The board is legal and correct but nothing in the ladder applies.
     *
     * Should never happen on a shipped puzzle, since every one was rated by the same ladder.
     * It can happen on a board the player has made unusual by hand, so it says so honestly
     * rather than pretending.
     */
    public data object Stuck : Hint
}

/**
 * Finds the next thing worth saying about a board.
 *
 * Works from the digits alone, never from the player's pencil marks. Their marks may be
 * out of date or simply wrong, and a hint built on a wrong premise is worse than no hint.
 */
public object HintEngine {

    public fun next(state: GameState, level: HintLevel = HintLevel.NAME): Hint {
        if (state.isSolved) return Hint.Solved

        // A wrong digit already on the board makes every later deduction meaningless, so it
        // has to be dealt with before anything else.
        val wrong = state.cells.indices.filter { index ->
            val digit = state.cells[index].digit
            digit != Board.EMPTY && digit != state.solution.atIndex(index)
        }
        if (wrong.isNotEmpty()) return Hint.Mistake(wrong.toSet())

        val grid = CandidateGrid.ofOrNull(state.toBoard()) ?: return Hint.Mistake(state.conflicts)
        if (grid.hasContradiction) return Hint.Mistake(state.conflicts)

        val deduction = Techniques.ladder.firstNotNullOfOrNull { it.find(grid) } ?: return Hint.Stuck
        return Hint.Step(deduction, level)
    }
}

/** The cells a hint wants lit up, split by what they mean. */
public fun Deduction.logicCells(): Set<Int> = focusCells.toSet()

public fun Deduction.struckCells(): Set<Int> = eliminations.map(CellDigit::cell).toSet()
