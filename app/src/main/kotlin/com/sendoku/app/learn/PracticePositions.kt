package com.sendoku.app.learn

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.Techniques
import com.sendoku.engine.technique.apply

/** A board, and the pattern the player is being asked to find in it. */
public data class Exercise(
    val technique: TechniqueId,
    val dims: Dimensions,
    val board: String,
    /** The cells the argument rests on. Tapping all of these, in any order, is a correct answer. */
    val cells: Set<Int>,
    /** What the pattern rules out, revealed after the answer rather than before it. */
    val eliminations: Set<Int>,
)

/**
 * Finds a position where a given technique applies.
 *
 * This is the piece that makes practice trustworthy. An exercise is not written by hand and
 * then checked; it is produced by asking the engine where the technique it is teaching
 * actually appears. An exercise can therefore never be wrong about its own answer, and the
 * app cannot ask somebody to find an X-Wing that is not there.
 *
 * It walks a puzzle forwards the way a solver does, and at every position asks the one
 * technique it cares about whether it applies to a grid built from the placed digits alone.
 * The digits only view matters: it is the same view the hint engine and the lesson board use,
 * so the pattern the player is asked for is the pattern they can actually see.
 */
public object PracticePositions {

    /** How far to walk one puzzle before giving up on it and taking the next. */
    private const val MAX_STEPS = 400

    /**
     * The first position in [puzzles] where [technique] applies, or null if none of them ever
     * shows it.
     *
     * Deliberately takes the puzzles as a sequence rather than reading the catalog itself, so
     * the caller decides how much work to do and the search can be tested against a handful.
     */
    public fun find(
        technique: TechniqueId,
        puzzles: Sequence<RatedPuzzle>,
        dims: Dimensions = Dimensions.CLASSIC,
    ): Exercise? {
        val finder = Techniques.ladder.firstOrNull { it.id == technique } ?: return null

        for (rated in puzzles) {
            val board = rated.puzzle.givens.copy()
            val walking = CandidateGrid.ofOrNull(board) ?: continue

            var steps = 0
            while (steps++ < MAX_STEPS) {
                val plain = CandidateGrid.ofOrNull(board) ?: break
                val found = finder.find(plain)
                if (found != null) return exercise(technique, dims, board, found)

                // Advance using the full view, which remembers eliminations, and mirror only
                // the placements onto the board, which is all a board can express.
                val step = Techniques.ladder.firstNotNullOfOrNull { it.find(walking) } ?: break
                walking.apply(step)
                for ((cell, digit) in step.placements) board.setAtIndex(cell, digit)
                if (step.placements.isEmpty() && step.eliminations.isEmpty()) break
            }
        }
        return null
    }

    private fun exercise(technique: TechniqueId, dims: Dimensions, board: Board, found: Deduction): Exercise = Exercise(
        technique = technique,
        dims = dims,
        board = (0 until dims.cellCount).joinToString("") {
            if (board.atIndex(it) == Board.EMPTY) "." else board.atIndex(it).toString()
        },
        cells = (found.focusCells + found.placements.map { it.cell }).toSet(),
        eliminations = found.eliminations.map { it.cell }.toSet(),
    )
}
