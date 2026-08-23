package com.sendoku.engine.catalog

import com.sendoku.engine.Grade

/**
 * What a puzzle has to look like to ship under a given grade.
 *
 * Difficulty itself is settled by the technique solver, not here. Measuring the generator
 * showed clue count and grade are close to unrelated: every band runs from about 22 clues
 * to about 33, with medians a cell or two apart. Anyone selecting difficulty by counting
 * clues is guessing, which is most of why other apps get it wrong.
 *
 * So these bounds are about how a board reads, not how hard it is. A Gentle puzzle with 22
 * clues is still gentle, but it looks forbidding, and a beginner will bounce off it before
 * discovering it was easy. [digFloor] stops the generator hollowing the easy grades out,
 * and [clues] throws away the stragglers that still come out wrong.
 */
public data class GradeSpec(val grade: Grade, val clues: IntRange, val digFloor: Int) {
    init {
        require(!clues.isEmpty()) { "${grade.displayName} has an empty clue range" }
        require(digFloor >= 0) { "${grade.displayName} has a negative dig floor" }
    }

    public fun accepts(clueCount: Int): Boolean = clueCount in clues

    public companion object {

        /**
         * Defaults for a nine by nine grid, taken from the measured distribution.
         *
         * The two easy grades stop digging early so the board stays welcoming. Everything
         * from Tricky up digs as far as uniqueness allows, because at that point a sparse
         * board is part of the appeal rather than a barrier.
         */
        public val defaults: Map<Grade, GradeSpec> = listOf(
            GradeSpec(Grade.GENTLE, clues = 32..50, digFloor = 34),
            GradeSpec(Grade.STEADY, clues = 28..44, digFloor = 30),
            GradeSpec(Grade.TRICKY, clues = 20..36, digFloor = 0),
            GradeSpec(Grade.SEVERE, clues = 20..36, digFloor = 0),
            GradeSpec(Grade.DIABOLICAL, clues = 20..34, digFloor = 0),
            GradeSpec(Grade.BEYOND, clues = 20..34, digFloor = 0),
        ).associateBy { it.grade }

        public fun of(grade: Grade): GradeSpec = defaults.getValue(grade)
    }
}
