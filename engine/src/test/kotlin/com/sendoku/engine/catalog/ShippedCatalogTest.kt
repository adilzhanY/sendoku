package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Solver
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.TechniqueSolver
import com.sendoku.engine.technique.Techniques
import org.junit.jupiter.api.Tag
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
@Tag("slow")
class ShippedCatalogTest {

    private val classic = Dimensions.CLASSIC

    private val catalog: PuzzleCatalog by lazy {
        val stream = checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")) {
            "the shipped catalog is missing, run ./gradlew :engine:generateCatalog"
        }
        stream.use { PuzzleFormat.read(it) }
    }

    @Test
    fun `the shipped batch holds five hundred of every grade`() {
        assertEquals(classic, catalog.dims)
        assertEquals(500 * Grade.entries.size, catalog.puzzles.size)
        for (grade in Grade.entries) {
            assertEquals(500, catalog.byGrade(grade).size, "${grade.displayName} count")
        }
    }

    @Test
    fun `no two shipped puzzles are the same, even in disguise`() {
        val givens = catalog.puzzles.map { it.puzzle.givens.toString() }
        assertEquals(givens.size, givens.toSet().size, "the batch repeats a puzzle outright")
        val solutions = catalog.puzzles.map { it.puzzle.solution.toString() }
        assertEquals(solutions.size, solutions.toSet().size, "two puzzles share a solution grid")

        // A relabelled or reflected copy solves identically, so a player would notice. Group
        // by a fingerprint no symmetry can change, then look properly at anything that
        // collides.
        val byPrint = catalog.puzzles.groupBy { GridEquivalence.fingerprint(it.puzzle.givens) }
        var compared = 0
        for ((_, group) in byPrint) {
            if (group.size < 2) continue
            for (i in group.indices) {
                for (j in i + 1 until group.size) {
                    compared++
                    assertTrue(
                        !GridEquivalence.areEquivalent(group[i].puzzle.givens, group[j].puzzle.givens),
                        "two shipped puzzles are the same grid in disguise",
                    )
                }
            }
        }
        println(
            "DUPLICATES ${byPrint.size} fingerprints over ${catalog.puzzles.size} " +
                "puzzles, $compared pairs checked in full",
        )
    }

    @Test
    fun `every grade genuinely demands its own level`() {
        // This is what a grade promises. A Severe puzzle that a Steady player could finish
        // would make the whole ladder meaningless, so each puzzle is re-solved with every
        // rule at or above its own level taken away, and must not come out.
        for ((index, rated) in catalog.puzzles.withIndex()) {
            val ceiling = rated.hardest?.cost ?: 0.0
            val easier = Techniques.ladder.filter { it.id.cost < ceiling }
            assertTrue(
                !TechniqueSolver(easier).solve(rated.puzzle.givens).isSolved,
                "puzzle $index is a ${rated.grade.displayName} that falls to cheaper rules",
            )
        }
    }

    @Test
    fun `every beyond puzzle really is beyond`() {
        // The headline claim. Beyond means the puzzle cannot be finished without a rule that
        // reasons about a group of cells as one, which is further than mainstream apps go.
        val beyond = catalog.byGrade(Grade.BEYOND)
        assertEquals(500, beyond.size)
        val withoutSetLogic = TechniqueSolver(Techniques.ladder.filter { it.id !in Techniques.setLogic })
        for ((index, rated) in beyond.withIndex()) {
            assertTrue(rated.hardest in Techniques.setLogic, "beyond puzzle $index rests on ${rated.hardest}")
            assertTrue(
                !withoutSetLogic.solve(rated.puzzle.givens).isSolved,
                "beyond puzzle $index can be solved without an almost locked set",
            )
        }
    }

    @Test
    fun `a sample of beyond puzzles, printed for a human to look at`() {
        // Hand sampling, as the roadmap asks. The numbers above prove the claim; this exists
        // so a person can actually read a few and see what the hardest grade looks like.
        for (rated in catalog.byGrade(Grade.BEYOND).take(3)) {
            val path = rated.usage.entries
                .sortedByDescending { it.key.cost }
                .joinToString(", ") { "${it.key.displayName} x${it.value}" }
            println("BEYOND ${rated.clueCount} clues, rating ${"%.2f".format(rated.rating)}, $path")
            println(rated.puzzle.givens.toString().replace("\n", "").chunked(9).joinToString("\n") { "  $it" })
        }
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
        assertTrue(bytes < 300_000, "the batch is $bytes bytes, which is too much of the download")
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
