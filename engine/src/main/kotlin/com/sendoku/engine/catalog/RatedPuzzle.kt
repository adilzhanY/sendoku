package com.sendoku.engine.catalog

import com.sendoku.engine.Grade
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueId

/**
 * A puzzle that has been through the technique solver, so its difficulty is known rather
 * than guessed.
 *
 * [usage] is the technique path summary: how many times each rule was needed. It is what
 * lets the app say "this one needs an X-Wing" on the win screen, and what makes it
 * possible to check later that a shipped batch really covers the range it claims.
 */
public data class RatedPuzzle(
    val puzzle: Puzzle,
    val rating: Double,
    val grade: Grade,
    val hardest: TechniqueId?,
    val symmetry: Symmetry,
    val usage: Map<TechniqueId, Int>,
) {
    val clueCount: Int get() = puzzle.clueCount

    /** Total number of deductions the solve took. */
    val stepCount: Int get() = usage.values.sum()
}
