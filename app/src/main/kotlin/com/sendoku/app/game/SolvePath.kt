package com.sendoku.app.game

import androidx.compose.runtime.Immutable
import com.sendoku.engine.Board
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.CellDigit
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.TechniqueSolver
import com.sendoku.engine.technique.Techniques

/**
 * The whole solution, step by step, as the engine found it.
 *
 * Only ever built for a game that is over. While a puzzle is live this would be the answer
 * with extra words, and the app would have spent every previous decision for nothing.
 *
 * It is the engine's own path rather than a retelling. The same walk down the ladder that
 * rated the puzzle produces this, so the hardest line in the list is the technique the
 * grade is named after, and a player can go and find that one line.
 */
@Immutable
public data class SolvePath(val steps: List<Step>) {

    /** One step: what was used, and what it did. */
    @Immutable
    public data class Step(
        val number: Int,
        val technique: TechniqueId,
        val placement: CellDigit?,
        val struck: Int,
        val size: Int,
    ) {
        /** True for the rules the deep end is made of, which are the ones worth going back to. */
        val advanced: Boolean get() = Grade.of(technique.cost).isAdvanced
    }

    public companion object {

        /**
         * Solves [givens] from scratch and records every step.
         *
         * Deliberately built from the puzzle's own clues rather than from wherever the
         * player got to. A path that starts from a half solved board would skip whatever
         * they had already found, and the interesting question after losing is what the
         * whole thing looked like, not what was left.
         */
        public fun of(givens: Board): SolvePath {
            val report = TechniqueSolver(Techniques.ladder).solve(givens)
            return SolvePath(
                report.steps.mapIndexed { index, step ->
                    Step(
                        number = index + 1,
                        technique = step.technique,
                        placement = step.placements.firstOrNull(),
                        struck = step.eliminations.size,
                        size = givens.dims.size,
                    )
                },
            )
        }
    }
}
