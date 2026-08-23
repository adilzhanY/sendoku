package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueSolver
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Tag("slow")
class BatchRunTest {

    private val classic = Dimensions.CLASSIC

    @Test
    fun `a small batch fills every grade it was asked for`() {
        val request = BatchRequest(
            targets = mapOf(Grade.GENTLE to 6, Grade.STEADY to 4, Grade.SEVERE to 4),
            seed = 301,
            workers = 4,
        )
        val result = BatchRun.run(request)
        assertTrue(result.met(request), "batch fell short:\n${result.summary()}")
        for ((grade, wanted) in request.targets) {
            assertEquals(wanted, result.counts[grade], "${grade.displayName} bucket")
        }
        assertEquals(request.targets.values.sum(), result.puzzles.size)
    }

    @Test
    fun `nothing in a batch is unsolvable, ambiguous or misfiled`() {
        val request = BatchRequest(
            targets = mapOf(Grade.GENTLE to 5, Grade.SEVERE to 5),
            seed = 303,
            workers = 4,
        )
        val brute = Solver(classic)
        val ladder = TechniqueSolver()
        for (rated in BatchRun.run(request).puzzles) {
            assertTrue(brute.hasUniqueSolution(rated.puzzle.givens), "a batch puzzle has two answers")
            val report = ladder.solve(rated.puzzle.givens)
            assertTrue(report.isSolved, "a batch puzzle cannot be solved by the ladder")
            assertEquals(rated.grade, report.grade, "a batch puzzle is filed under the wrong grade")
            assertEquals(rated.puzzle.solution, report.board)
            assertTrue(GradeSpec.of(rated.grade).accepts(rated.clueCount))
        }
    }

    @Test
    fun `a batch comes back sorted by grade then rating`() {
        val result = BatchRun.run(
            BatchRequest(targets = mapOf(Grade.GENTLE to 5, Grade.STEADY to 5), seed = 305, workers = 4),
        )
        val ordinals = result.puzzles.map { it.grade.ordinal }
        assertEquals(ordinals.sorted(), ordinals)
        for (grade in Grade.entries) {
            val ratings = result.puzzles.filter { it.grade == grade }.map { it.rating }
            assertEquals(ratings.sorted(), ratings, "${grade.displayName} is out of order")
        }
    }

    @Test
    fun `an impossible target gives up instead of running forever`() {
        val request = BatchRequest(
            targets = mapOf(Grade.BEYOND to 10_000),
            seed = 307,
            workers = 2,
            maxAttempts = 400,
        )
        val result = BatchRun.run(request)
        assertTrue(!result.met(request))
        assertTrue(result.attempts <= 400 + request.workers, "ran ${result.attempts} attempts past the cap")
    }

    @Test
    fun `the summary says what happened`() {
        val result = BatchRun.run(
            BatchRequest(targets = mapOf(Grade.GENTLE to 4), seed = 309, workers = 2),
        )
        val summary = result.summary()
        assertTrue(summary.contains("Gentle"), summary)
        assertTrue(summary.contains("clues"), summary)
        assertTrue(result.attempts >= result.puzzles.size)
    }

    @Test
    fun `a batch round trips through the file format`() {
        val result = BatchRun.run(
            BatchRequest(targets = mapOf(Grade.GENTLE to 4, Grade.STEADY to 3), seed = 311, workers = 3),
        )
        val bytes = java.io.ByteArrayOutputStream()
        PuzzleFormat.write(bytes, classic, result.puzzles)
        val catalog = PuzzleFormat.read(java.io.ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(result.counts, catalog.counts)
        assertEquals(result.puzzles.map { it.puzzle.givens }, catalog.puzzles.map { it.puzzle.givens })
    }

    @Test
    fun `a nonsense request is refused up front`() {
        assertFailsWith<IllegalArgumentException> { BatchRequest(targets = emptyMap()) }
        assertFailsWith<IllegalArgumentException> { BatchRequest(targets = mapOf(Grade.GENTLE to 0)) }
        assertFailsWith<IllegalArgumentException> {
            BatchRequest(targets = mapOf(Grade.GENTLE to 1), workers = 0)
        }
    }

    @Test
    fun `symmetry is carried through to the shipped puzzle`() {
        val result = BatchRun.run(
            BatchRequest(
                targets = mapOf(Grade.GENTLE to 4),
                symmetry = Symmetry.DIAGONAL,
                seed = 313,
                workers = 2,
            ),
        )
        assertTrue(result.puzzles.all { it.symmetry == Symmetry.DIAGONAL })
    }
}
