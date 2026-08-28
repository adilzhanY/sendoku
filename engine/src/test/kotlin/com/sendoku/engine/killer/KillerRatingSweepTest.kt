package com.sendoku.engine.killer

import com.sendoku.engine.Dimensions
import com.sendoku.engine.technique.SolveOutcome
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How much of Killer the ladder can actually reach.
 *
 * The number this reports is the whole reason the cage rules exist: a Killer the ladder
 * cannot finish is one Sendoku cannot rate, cannot hint at and will not ship. If it ever
 * falls, a rule has been broken rather than merely missed, and the batch would quietly get
 * smaller.
 */
@Tag("slow")
class KillerRatingSweepTest {

    @Test
    fun `the ladder finishes most generated killers, and every one it finishes it finishes correctly`() {
        val dims = Dimensions.CLASSIC
        var made = 0
        var solved = 0
        val grades = HashMap<String, Int>()

        for (seed in 1..40) {
            val puzzle = KillerGenerator(dims, Random(seed)).next() ?: continue
            made++
            val report = KillerRater(puzzle).solve()
            if (!report.isSolved) {
                assertEquals(SolveOutcome.STUCK, report.outcome, "seed $seed ended badly")
                continue
            }
            solved++
            assertEquals(puzzle.solution.toString(), report.board.toString(), "seed $seed solved wrongly")
            grades[report.grade.name] = (grades[report.grade.name] ?: 0) + 1
        }

        println("KILLER made=$made solved=$solved grades=$grades")
        assertTrue(made > 0, "the generator produced nothing at all")
        assertTrue(
            solved * 2 >= made,
            "the ladder finished only $solved of $made killers, so most of them could not be rated",
        )
    }
}
