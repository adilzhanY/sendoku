package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.technique.TechniqueSolver
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("slow")
class PuzzleSupplyTest {

    private val classic = Dimensions.CLASSIC

    private fun reader(): CatalogReader =
        checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")).use { CatalogReader.from(it) }

    private fun supply(seed: Long = 701, attempts: Int = 4_000) =
        PuzzleSupply(reader(), GradedGenerator(classic, Random(seed)), liveAttempts = attempts)

    @Test
    fun `a fresh device gets a puzzle straight from the batch`() {
        val supply = supply()
        for (grade in Grade.entries) {
            val supplied = supply.take(grade, played = emptySet(), random = Random(1))
            assertTrue(supplied is Supplied.FromCatalog, "${grade.displayName} did not come from the batch")
            assertEquals(grade, supplied.puzzle.grade)
        }
    }

    @Test
    fun `a puzzle already played is never handed out again while others remain`() {
        val supply = supply()
        val played = HashSet<Int>()
        repeat(40) {
            val supplied = supply.take(Grade.SEVERE, played, Random(800L + it))
            assertTrue(supplied is Supplied.FromCatalog)
            assertTrue(supplied.index !in played, "index ${supplied.index} came back a second time")
            played.add(supplied.index)
        }
        assertEquals(40, played.size)
    }

    @Test
    fun `remaining counts down as the batch is played`() {
        val supply = supply()
        val all = reader().indicesOf(Grade.TRICKY)
        assertEquals(all.size, supply.remaining(Grade.TRICKY))
        assertEquals(all.size - 3, supply.remaining(Grade.TRICKY, all.take(3).toSet()))
        assertEquals(0, supply.remaining(Grade.TRICKY, all.toSet()))
    }

    @Test
    fun `running the batch dry falls through to generating on the device`() {
        val supply = supply()
        val everything = reader().indicesOf(Grade.GENTLE).toSet()
        val supplied = supply.take(Grade.GENTLE, everything, Random(2))

        assertTrue(supplied is Supplied.GeneratedLive, "expected a live puzzle, got $supplied")
        assertEquals(Grade.GENTLE, supplied.puzzle.grade)
        assertTrue(Solver(classic).hasUniqueSolution(supplied.puzzle.puzzle.givens))
        assertTrue(TechniqueSolver().solve(supplied.puzzle.puzzle.givens).isSolved)
    }

    @Test
    fun `a live puzzle is held to exactly the same standard as a shipped one`() {
        val supply = supply(seed = 703)
        val everything = reader().indicesOf(Grade.SEVERE).toSet()
        val supplied = supply.take(Grade.SEVERE, everything, Random(3))
        val rated = supplied.puzzle

        assertEquals(Grade.SEVERE, rated.grade)
        assertTrue(GradeSpec.of(Grade.SEVERE).accepts(rated.clueCount))
        val report = TechniqueSolver().solve(rated.puzzle.givens)
        assertTrue(report.isSolved)
        assertEquals(rated.puzzle.solution, report.board)
        assertEquals(rated.rating, report.rating, absoluteTolerance = 1e-9)
    }

    @Test
    fun `when even generating fails the player still gets a puzzle`() {
        // One attempt will not find a Beyond puzzle, so the last resort has to fire.
        val supply = supply(seed = 705, attempts = 1)
        val everything = reader().indicesOf(Grade.BEYOND).toSet()
        val supplied = supply.take(Grade.BEYOND, everything, Random(4))

        assertTrue(
            supplied is Supplied.Recycled || supplied is Supplied.GeneratedLive,
            "the player was left with nothing",
        )
        assertEquals(Grade.BEYOND, supplied.puzzle.grade)
    }

    @Test
    fun `recycling says so rather than pretending the puzzle is new`() {
        val supply = supply(seed = 707, attempts = 1)
        val everything = reader().indicesOf(Grade.BEYOND).toSet()
        var recycled = 0
        repeat(6) {
            if (supply.take(Grade.BEYOND, everything, Random(900L + it)) is Supplied.Recycled) recycled++
        }
        assertTrue(recycled > 0, "nothing was ever marked as recycled, so the app cannot tell the player")
    }

    @Test
    fun `every grade can be supplied over and over without repeating early`() {
        val supply = supply(seed = 709)
        for (grade in Grade.entries) {
            val played = HashSet<Int>()
            repeat(15) {
                val supplied = supply.take(grade, played, Random(1000L + it))
                assertEquals(grade, supplied.puzzle.grade, "${grade.displayName} handed out the wrong grade")
                if (supplied is Supplied.FromCatalog) played.add(supplied.index)
            }
            assertEquals(15, played.size, "${grade.displayName} repeated a puzzle")
        }
    }
}
