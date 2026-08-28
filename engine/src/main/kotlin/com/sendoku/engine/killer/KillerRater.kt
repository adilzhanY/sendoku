package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.SolveOutcome
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.TechniqueSolver
import com.sendoku.engine.technique.Techniques
import com.sendoku.engine.technique.apply

/** How a Killer puzzle was solved, and therefore how hard it is. */
public data class KillerReport(
    val outcome: SolveOutcome,
    val board: Board,
    val steps: List<Deduction>,
    val rating: Double,
    val grade: Grade,
    val hardest: TechniqueId?,
) {
    public val isSolved: Boolean get() = outcome == SolveOutcome.SOLVED

    /** How many times each rule was needed, which is what the win screen names. */
    public val usage: Map<TechniqueId, Int> get() = steps.groupingBy { it.technique }.eachCount()
}

/**
 * Solves a Killer the way a person would, and reports how hard that was.
 *
 * The same walk as the ordinary [TechniqueSolver], over a ladder with the cage rules mixed
 * into it by cost. That mixing is the whole point: a Killer that only ever needed cage sums
 * and hidden singles is an easy puzzle and should be called one, and a Killer that needed a
 * Swordfish is hard for exactly the reason an ordinary puzzle needing a Swordfish is hard.
 * Two ladders would have produced two incomparable difficulty scales in one app.
 *
 * There is no fall back to brute force here either. If the rules run out, the honest answer
 * is that this puzzle is beyond the ladder, and Sendoku does not ship a puzzle it cannot
 * explain.
 */
public class KillerRater(private val puzzle: KillerPuzzle) {

    /** Every rule, ordinary and cage alike, cheapest first. */
    private val ladder: List<Step> = buildList {
        for (technique in Techniques.ladder) add(Step.Plain(technique))
        for (technique in CageTechniques.ladder) add(Step.Cage(technique))
    }.sortedBy { it.cost }

    public fun solve(): KillerReport {
        val grid = CandidateGrid.ofOrNull(Board(puzzle.dims))
            ?: return report(SolveOutcome.ILLEGAL, Board(puzzle.dims), emptyList())

        val steps = ArrayList<Deduction>()
        while (!grid.isSolved) {
            if (grid.hasContradiction) return report(SolveOutcome.CONTRADICTION, grid.toBoard(), steps)
            val step = ladder.firstNotNullOfOrNull { it.find(grid, puzzle) }
                ?: return report(SolveOutcome.STUCK, grid.toBoard(), steps)
            grid.apply(step)
            steps.add(step)
        }
        return report(SolveOutcome.SOLVED, grid.toBoard(), steps)
    }

    private fun report(outcome: SolveOutcome, board: Board, steps: List<Deduction>): KillerReport {
        val rating = TechniqueSolver.ratingOf(steps)
        return KillerReport(
            outcome = outcome,
            board = board,
            steps = steps.toList(),
            rating = rating,
            grade = Grade.of(rating),
            hardest = steps.maxByOrNull { it.technique.cost }?.technique,
        )
    }

    /** One rung of the mixed ladder: an ordinary rule or a cage one. */
    private sealed interface Step {
        val cost: Double

        fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction?

        data class Plain(val technique: com.sendoku.engine.technique.Technique) : Step {
            override val cost: Double get() = technique.id.cost
            override fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction? = technique.find(grid)
        }

        data class Cage(val technique: CageTechnique) : Step {
            override val cost: Double get() = technique.id.cost
            override fun find(grid: CandidateGrid, puzzle: KillerPuzzle): Deduction? = technique.find(grid, puzzle)
        }
    }
}
