package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Grade

/** How a technique solve ended. */
public enum class SolveOutcome {
    /** Every cell filled, using nothing but the rules in the ladder. */
    SOLVED,

    /** The rules ran out before the grid did. The puzzle is harder than Sendoku can rate. */
    STUCK,

    /** A cell lost every candidate, so the starting board cannot be completed. */
    CONTRADICTION,

    /** The starting board already repeats a digit somewhere. */
    ILLEGAL,
}

/**
 * What a technique solve produced.
 *
 * [rating] and [grade] only mean something when [outcome] is [SolveOutcome.SOLVED]. For a
 * stuck grid they describe the ground that was covered before the rules ran out, which is
 * useful for reporting but is not a difficulty.
 */
public data class SolveReport(
    val outcome: SolveOutcome,
    val board: Board,
    val steps: List<Deduction>,
    val rating: Double,
    val grade: Grade,
    val hardest: TechniqueId?,
) {
    public val isSolved: Boolean get() = outcome == SolveOutcome.SOLVED

    /** How many times each technique was needed. */
    public val usage: Map<TechniqueId, Int>
        get() = steps.groupingBy { it.technique }.eachCount()
}

/**
 * Solves a puzzle the way a person would, and reports how hard that was.
 *
 * It walks the ladder from the cheapest rule up, applies the first step it finds, and
 * starts again from the bottom. That is what a player does, and it is what makes the
 * resulting path meaningful: a rule only appears in the path when nothing simpler would
 * have worked at that moment.
 *
 * There is deliberately no fall back to the backtracking [com.sendoku.engine.Solver]. If
 * the rules run out, the honest answer is that this puzzle is beyond the ladder, and
 * Sendoku will not ship a puzzle it cannot explain.
 */
public class TechniqueSolver(
    private val ladder: List<Technique> = Techniques.ladder,
) {

    /** Runs the ladder over [board] until it is solved or nothing applies. */
    public fun solve(board: Board): SolveReport {
        val grid = CandidateGrid.ofOrNull(board)
            ?: return report(SolveOutcome.ILLEGAL, board, emptyList())

        val steps = ArrayList<Deduction>()
        while (!grid.isSolved) {
            if (grid.hasContradiction) {
                return report(SolveOutcome.CONTRADICTION, grid.toBoard(), steps)
            }
            val step = ladder.firstNotNullOfOrNull { it.find(grid) }
                ?: return report(SolveOutcome.STUCK, grid.toBoard(), steps)
            grid.apply(step)
            steps.add(step)
        }
        return report(SolveOutcome.SOLVED, grid.toBoard(), steps)
    }

    /** The difficulty of [board], or null when the ladder cannot finish it. */
    public fun rate(board: Board): Double? = solve(board).takeIf { it.isSolved }?.rating

    /** The grade of [board], or null when the ladder cannot finish it. */
    public fun grade(board: Board): Grade? = solve(board).takeIf { it.isSolved }?.grade

    private fun report(outcome: SolveOutcome, board: Board, steps: List<Deduction>): SolveReport {
        val rating = ratingOf(steps)
        return SolveReport(
            outcome = outcome,
            board = board,
            steps = steps.toList(),
            rating = rating,
            grade = Grade.of(rating),
            hardest = steps.maxByOrNull { it.technique.cost }?.technique,
        )
    }

    public companion object {
        /**
         * Most a puzzle can gain from leaning on its hardest rule over and over.
         *
         * Deliberately smaller than the gap between any two technique costs. If the bonus
         * could ever reach the next rule's cost, repetition alone would move a puzzle into
         * the band above, and a grade would stop meaning "this is what you have to know".
         */
        public const val MAX_REPETITION_BONUS: Double = 0.09

        private const val BONUS_PER_EXTRA_STEP = 0.01

        /**
         * The difficulty of a solve path.
         *
         * The hardest rule the puzzle forced you to use sets the number. Needing it once
         * is not the same as needing it eight times, though, so each extra use at the top
         * of the path adds a little, up to [MAX_REPETITION_BONUS].
         *
         * The bonus is kept small on purpose. A puzzle that needs one X-Wing and a puzzle
         * that needs five are both X-Wing puzzles, and no amount of repetition should push
         * either into the band above.
         */
        public fun ratingOf(steps: List<Deduction>): Double {
            if (steps.isEmpty()) return 0.0
            val hardest = steps.maxOf { it.technique.cost }
            val atTheTop = steps.count { it.technique.cost == hardest }
            val bonus = minOf(MAX_REPETITION_BONUS, BONUS_PER_EXTRA_STEP * (atTheTop - 1))
            return hardest + bonus
        }
    }
}
