package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.technique.TechniqueSolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the batch that actually ships.
 *
 * This is the last gate before a puzzle reaches a player. Every other test checks that the
 * machinery is right; this one checks the output. A single ambiguous or unsolvable puzzle
 * in here would be a one star review, and no amount of green tests elsewhere would have
 * caught it.
 *
 * It solves all two thousand puzzles twice over, by brute force and by the ladder, which
 * costs a few seconds. That is the right trade for the one artefact the player receives.
 */
class ShippedCatalogTest {

    private val classic = Dimensions.CLASSIC

    private val catalog: PuzzleCatalog by lazy {
        val stream = checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")) {
            "the shipped catalog is missing, run ./gradlew :engine:generateCatalog"
        }
        stream.use { PuzzleFormat.read(it) }
    }

    @Test
    fun `the shipped batch holds five hundred of each grade up to severe`() {
        assertEquals(classic, catalog.dims)
        assertEquals(2000, catalog.puzzles.size)
        for (grade in listOf(Grade.GENTLE, Grade.STEADY, Grade.TRICKY, Grade.SEVERE)) {
            assertEquals(500, catalog.byGrade(grade).size, "${grade.displayName} count")
        }
    }

    @Test
    fun `no two shipped puzzles are the same`() {
        val givens = catalog.puzzles.map { it.puzzle.givens.toString() }
        assertEquals(givens.size, givens.toSet().size, "the batch repeats a puzzle")
        val solutions = catalog.puzzles.map { it.puzzle.solution.toString() }
        assertEquals(solutions.size, solutions.toSet().size, "two puzzles share a solution grid")
    }

    @Test
    fun `every shipped puzzle has exactly one answer`() {
        val brute = Solver(classic)
        for ((index, rated) in catalog.puzzles.withIndex()) {
            assertTrue(
                brute.hasUniqueSolution(rated.puzzle.givens),
                "puzzle $index has more than one answer",
            )
        }
    }

    @Test
    fun `every shipped puzzle is solvable by reasoning, and filed correctly`() {
        val ladder = TechniqueSolver()
        for ((index, rated) in catalog.puzzles.withIndex()) {
            val report = ladder.solve(rated.puzzle.givens)
            assertTrue(report.isSolved, "puzzle $index cannot be solved without guessing")
            assertEquals(rated.puzzle.solution, report.board, "puzzle $index stores the wrong solution")
            assertEquals(rated.grade, report.grade, "puzzle $index is filed under the wrong grade")
            assertEquals(rated.rating, report.rating, absoluteTolerance = 0.005, message = "puzzle $index")
            assertEquals(rated.hardest, report.hardest, "puzzle $index names the wrong hardest rule")
        }
    }

    @Test
    fun `every shipped puzzle looks the way its grade should`() {
        for ((index, rated) in catalog.puzzles.withIndex()) {
            assertTrue(
                GradeSpec.of(rated.grade).accepts(rated.clueCount),
                "puzzle $index is a ${rated.grade.displayName} with ${rated.clueCount} clues",
            )
            assertEquals(Grade.of(rated.rating), rated.grade, "puzzle $index")
            assertTrue(rated.stepCount > 0, "puzzle $index needed no steps at all")
        }
    }

    @Test
    fun `the batch is small enough to ship`() {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb"))
            .use { it.readBytes().size }
        println("CATALOG ${catalog.puzzles.size} puzzles in $bytes bytes, ${bytes / catalog.puzzles.size} each")
        assertTrue(bytes < 250_000, "the batch is $bytes bytes, which is too much of the download")
    }

    @Test
    fun `the batch spans a real range of difficulty within each grade`() {
        // A grade full of puzzles that all rate identically would be technically correct and
        // dull to play, so the spread is checked rather than assumed.
        for (grade in listOf(Grade.GENTLE, Grade.STEADY, Grade.TRICKY, Grade.SEVERE)) {
            val ratings = catalog.byGrade(grade).map { it.rating }
            assertTrue(
                ratings.max() - ratings.min() > 0.2,
                "${grade.displayName} spans only ${ratings.max() - ratings.min()}",
            )
            val techniques = catalog.byGrade(grade).mapNotNull { it.hardest }.toSet()
            assertTrue(techniques.size >= 2, "${grade.displayName} only ever needs $techniques")
        }
    }
}
