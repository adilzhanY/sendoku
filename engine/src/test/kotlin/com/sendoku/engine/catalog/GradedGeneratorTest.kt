package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.SolveOutcome
import com.sendoku.engine.technique.TechniqueSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GradedGeneratorTest {

    private val classic = Dimensions.CLASSIC

    @Test
    fun `a rated puzzle is rated correctly`() {
        val maker = GradedGenerator(classic, Random(101))
        val solver = TechniqueSolver()
        var checked = 0
        repeat(40) {
            val rated = maker.next() ?: return@repeat
            checked++
            val report = solver.solve(rated.puzzle.givens)
            assertEquals(SolveOutcome.SOLVED, report.outcome)
            assertEquals(report.rating, rated.rating)
            assertEquals(report.grade, rated.grade)
            assertEquals(report.hardest, rated.hardest)
            assertEquals(report.usage, rated.usage)
            assertEquals(rated.puzzle.clueCount, rated.clueCount)
            assertEquals(report.steps.size, rated.stepCount)
        }
        assertTrue(checked > 30, "only $checked of 40 puzzles could be rated at all")
    }

    @Test
    fun `an unrateable puzzle is dropped rather than shipped`() {
        val maker = GradedGenerator(classic, Random(103))
        val solver = TechniqueSolver()
        repeat(60) {
            val rated = maker.next(Symmetry.NONE) ?: return@repeat
            assertTrue(solver.solve(rated.puzzle.givens).isSolved)
        }
    }

    @Test
    fun `asking for a grade gets that grade`() {
        val maker = GradedGenerator(classic, Random(107))
        for (grade in listOf(Grade.GENTLE, Grade.STEADY, Grade.SEVERE)) {
            val rated = assertNotNull(
                maker.generate(grade, attempts = 4000),
                "could not make a ${grade.displayName} puzzle in four thousand tries",
            )
            assertEquals(grade, rated.grade)
            assertEquals(grade, Grade.of(rated.rating))
            assertTrue(GradeSpec.of(grade).accepts(rated.clueCount))
            assertTrue(Solver(classic).hasUniqueSolution(rated.puzzle.givens))
        }
    }

    @Test
    fun `the easy grades come out with a friendly number of clues`() {
        val maker = GradedGenerator(classic, Random(109))
        val gentle = assertNotNull(maker.generate(Grade.GENTLE, attempts = 2000))
        assertTrue(gentle.clueCount >= 32, "a gentle puzzle with only ${gentle.clueCount} clues looks hard")
    }

    @Test
    fun `giving up returns nothing rather than the wrong grade`() {
        val maker = GradedGenerator(classic, Random(111))
        val result = maker.generate(Grade.BEYOND, attempts = 1)
        if (result != null) assertEquals(Grade.BEYOND, result.grade)
    }

    @Test
    fun `every grade has a spec and the specs are sane`() {
        assertEquals(Grade.entries.toSet(), GradeSpec.defaults.keys)
        for (grade in Grade.entries) {
            val spec = GradeSpec.of(grade)
            assertEquals(grade, spec.grade)
            assertTrue(spec.clues.first >= 17, "${grade.displayName} allows fewer clues than any proper puzzle")
            assertTrue(spec.clues.last <= 81)
            assertTrue(spec.digFloor <= spec.clues.last)
            if (spec.digFloor > 0) {
                assertTrue(
                    spec.digFloor >= spec.clues.first,
                    "${grade.displayName} digs below its own clue floor",
                )
            }
        }
    }

    @Test
    fun `the easy grades stop digging and the hard ones do not`() {
        assertTrue(GradeSpec.of(Grade.GENTLE).digFloor > 0)
        assertTrue(GradeSpec.of(Grade.STEADY).digFloor > 0)
        assertEquals(0, GradeSpec.of(Grade.SEVERE).digFloor)
        assertEquals(0, GradeSpec.of(Grade.BEYOND).digFloor)
    }

    @Test
    fun `the same seed makes the same puzzle`() {
        val first = GradedGenerator(classic, Random(113)).next()
        val second = GradedGenerator(classic, Random(113)).next()
        assertEquals(first?.puzzle, second?.puzzle)
        assertEquals(first?.rating, second?.rating)
    }
}
