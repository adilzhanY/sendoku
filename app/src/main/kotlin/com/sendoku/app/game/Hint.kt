package com.sendoku.app.game

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Geometry
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import com.sendoku.engine.technique.CellDigit
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId
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
    /**
     * The region, and nothing else. "There is something in box four."
     *
     * The nudge before the nudge. Most of the time a player is not stuck on the reasoning,
     * they are stuck on where to point their eyes, and being told which ninth of the grid to
     * read gives them the whole of the rest to find for themselves.
     */
    REGION,

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

    /**
     * A move that follows from the board, and the reasoning behind it.
     *
     * [restsOnEarlierHints] is true when part of the argument is a candidate an earlier hint
     * ruled out rather than something a player can read off the digits. The panel says so
     * when it is, because a hint that quietly assumes its own earlier work reads as the app
     * telling you to guess.
     */
    public data class Step(
        val deduction: Deduction,
        val level: HintLevel,
        val restsOnEarlierHints: Boolean = false,
        /** The arithmetic behind a single, when the step is one. See [Evidence]. */
        val evidence: Evidence? = null,
    ) : Hint

    /**
     * What actually ruled the other digits out.
     *
     * A hint that says "only one digit is left here" and stops is asking to be believed. The
     * player is looking at a grid where the digit in question could still go in thirty other
     * cells, which is a different question with a different answer, and without the working
     * there is no way to tell that both are true.
     *
     * So the working goes on screen: what the row already holds, what the column holds, what
     * the box holds, and the one digit none of them has. Three lists a person can check in
     * about four seconds, against a board that has the three houses outlined for them.
     */
    public data class Evidence(val digit: Int, val row: List<Int>, val column: List<Int>, val box: List<Int>)

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
 *
 * The one thing it does carry over is what earlier hints have ruled out, held in the state
 * and thrown away the moment the player touches the board. Without it an elimination hint
 * can never lead anywhere: the next call rebuilds the same board and says the same thing.
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
        for ((cell, digit) in state.eliminated) grid.eliminate(cell, digit)
        if (grid.hasContradiction) return Hint.Mistake(state.conflicts)

        // Skip anything that would change nothing. A deduction whose eliminations are all
        // already known is true and useless, and offering it again is how the loop started.
        val deduction = Techniques.ladder.firstNotNullOfOrNull { technique ->
            technique.find(grid)?.takeIf {
                it.placements.isNotEmpty() ||
                    it.eliminations.any { e -> e !in state.eliminated }
            }
        } ?: return Hint.Stuck
        return Hint.Step(
            deduction = deduction,
            level = level,
            restsOnEarlierHints = restsOnEarlierHints(state, grid, deduction),
            evidence = evidenceFor(state, deduction),
        )
    }

    /** The digits already spoken for in the three houses of a naked single's cell. */
    private fun evidenceFor(state: GameState, deduction: Deduction): Hint.Evidence? {
        if (deduction.technique != TechniqueId.NAKED_SINGLE) return null
        val (cell, digit) = deduction.placements.firstOrNull() ?: return null
        val size = state.size
        val geometry = Geometry.of(state.dims)
        fun digitsIn(house: House) = geometry.cellsOf(house)
            .map { state.cells[it].digit }
            .filter { it != Board.EMPTY }
            .distinct()
            .sorted()

        return Hint.Evidence(
            digit = digit,
            row = digitsIn(House(HouseKind.ROW, cell / size)),
            column = digitsIn(House(HouseKind.COLUMN, cell % size)),
            box = digitsIn(House(HouseKind.BOX, state.dims.boxOf(cell / size, cell % size))),
        )
    }

    /**
     * Whether the argument leans on a candidate the board alone does not rule out.
     *
     * Anything the deduction points at counts: the cells it rests on, the cells it strikes,
     * and every cell of the houses it names, since a hidden single's argument is about the
     * whole house rather than about one cell. If an earlier hint ruled a digit out of any of
     * them, the player cannot check this one by reading the grid, and has to be told so.
     */
    private fun restsOnEarlierHints(state: GameState, grid: CandidateGrid, deduction: Deduction): Boolean {
        if (state.eliminated.isEmpty()) return false
        val relevant = buildSet {
            addAll(deduction.focusCells)
            addAll(deduction.eliminations.map { it.cell })
            for (house in deduction.houses) addAll(grid.cellsOf(house).toList())
        }
        return state.eliminated.any { it.cell in relevant }
    }
}

/** The cells a hint wants lit up, split by what they mean. */
public fun Deduction.logicCells(): Set<Int> = focusCells.toSet()

public fun Deduction.struckCells(): Set<Int> = eliminations.map(CellDigit::cell).toSet()
