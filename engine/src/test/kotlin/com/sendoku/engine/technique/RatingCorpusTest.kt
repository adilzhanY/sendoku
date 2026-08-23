package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.Grade
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Two hundred puzzles, rated once and written down.
 *
 * A difficulty rating is a promise to the player: a Severe puzzle should feel like the
 * last Severe puzzle. Nothing else in the engine guards that. A technique can be made
 * slightly stronger, or a cost nudged, and every grade in the app shifts underneath the
 * player with no test going red.
 *
 * So the grades are pinned. If this fails, the rating changed, and the only question is
 * whether that was on purpose. If it was, regenerate the file with [regenerate] below and
 * read the diff before committing it.
 */
class RatingCorpusTest {

    private data class Entry(
        val givens: String,
        val outcome: SolveOutcome,
        val rating: Double,
        val grade: Grade,
        val hardest: String,
    )

    private val corpus: List<Entry> by lazy {
        val text = checkNotNull(javaClass.getResourceAsStream("/rating-corpus.csv")) {
            "rating-corpus.csv is missing from the test resources"
        }.bufferedReader().readText()

        text.lineSequence().filter { it.isNotBlank() }.map { line ->
            val parts = line.split(",")
            require(parts.size == 5) { "malformed corpus line: $line" }
            Entry(
                givens = parts[0],
                outcome = SolveOutcome.valueOf(parts[1]),
                rating = parts[2].toDouble(),
                grade = Grade.valueOf(parts[3]),
                hardest = parts[4],
            )
        }.toList()
    }

    @Test
    fun `the corpus is two hundred puzzles`() {
        assertEquals(200, corpus.size)
        assertEquals(200, corpus.map { it.givens }.toSet().size, "the corpus repeats a puzzle")
        for (entry in corpus) assertEquals(81, entry.givens.length)
    }

    @Test
    fun `no grade has drifted`() {
        val solver = TechniqueSolver()
        val drifted = ArrayList<String>()
        for (entry in corpus) {
            val report = solver.solve(Board.parse(Dimensions.CLASSIC, entry.givens))
            if (report.outcome != entry.outcome ||
                report.grade != entry.grade ||
                abs(report.rating - entry.rating) > 0.005 ||
                (report.hardest?.name ?: "-") != entry.hardest
            ) {
                drifted += "${entry.givens.take(20)}... was ${entry.outcome}/${entry.rating}/" +
                    "${entry.grade}/${entry.hardest}, now ${report.outcome}/" +
                    String.format(java.util.Locale.ROOT, "%.2f", report.rating) +
                    "/${report.grade}/${report.hardest ?: "-"}"
            }
        }
        assertTrue(
            drifted.isEmpty(),
            "${drifted.size} of ${corpus.size} puzzles changed rating:\n" + drifted.take(10).joinToString("\n"),
        )
    }

    @Test
    fun `the corpus still covers the whole ladder it claims to`() {
        // A corpus that quietly collapsed to nothing but singles would pass the drift check
        // while testing almost nothing, so the spread is pinned too.
        val grades = corpus.map { it.grade }.toSet()
        assertEquals(Grade.entries.toSet(), grades, "the corpus no longer reaches every grade")
        assertTrue(
            corpus.map { it.hardest }.toSet().size >= 14,
            "the corpus exercises too few techniques",
        )
        assertTrue(corpus.count { it.outcome == SolveOutcome.SOLVED } >= 190)
    }

    @Test
    fun `every rating in the corpus matches its own grade`() {
        for (entry in corpus) {
            if (entry.outcome != SolveOutcome.SOLVED) continue
            assertEquals(Grade.of(entry.rating), entry.grade, entry.givens)
        }
    }

    /**
     * Rewrites the corpus. Un-ignore, run, and paste the printed block over
     * `engine/src/test/resources/rating-corpus.csv`.
     *
     * Only do this when a rating change was deliberate, and read the diff first.
     */
    @Ignore
    @Test
    fun regenerate() {
        val solver = TechniqueSolver()
        for (seed in 10_000 until 10_200) {
            val puzzle = Generator(Dimensions.CLASSIC, Random(seed.toLong())).generate()
            val report = solver.solve(puzzle.givens)
            val rating = String.format(java.util.Locale.ROOT, "%.2f", report.rating)
            println(
                puzzle.givens.toString().replace("\n", "") +
                    ",${report.outcome},$rating,${report.grade},${report.hardest ?: "-"}",
            )
        }
    }

    @Test
    fun `the corpus reproduces from its seeds`() {
        // The file is not hand written, and this is what says so. If the generator changes,
        // this fails before the drift check does, which points at the real cause.
        val first = assertNotNull(corpus.firstOrNull())
        val regenerated = Generator(Dimensions.CLASSIC, Random(10_000L)).generate()
        assertEquals(first.givens, regenerated.givens.toString().replace("\n", ""))
    }
}
