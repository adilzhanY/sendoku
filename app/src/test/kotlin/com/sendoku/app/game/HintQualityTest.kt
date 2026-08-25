package com.sendoku.app.game

import com.sendoku.app.learn.PracticePositions
import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.technique.Deduction
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.Techniques
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * What a hint has to carry before a rule may ship behind it.
 *
 * A technique that fires correctly and cannot be drawn is a technique that produces a hint
 * saying "there is an ALS-XY-Wing somewhere, good luck". The engine is free to reason any
 * way it likes; anything that reaches a player has to name the cells it rests on and say
 * what it changes, or the app is asserting rather than teaching.
 */
class HintQualityTest {

    private fun puzzles(count: Int) = GradedGenerator(Dimensions.CLASSIC, Random(5)).let { maker ->
        generateSequence { maker.next(Symmetry.ROTATIONAL) }.take(count)
    }

    /** A real deduction for each rule, found in real puzzles rather than drawn by hand. */
    private val examples: Map<TechniqueId, Deduction> by lazy {
        val found = HashMap<TechniqueId, Deduction>()
        for (id in TechniqueId.entries) {
            val exercise = PracticePositions.find(id, puzzles(400), Dimensions.CLASSIC) ?: continue
            val grid = CandidateGrid.of(com.sendoku.engine.Board.parse(exercise.dims, exercise.board))
            found[id] = Techniques.byId(id)?.find(grid) ?: continue
        }
        found
    }

    @Test
    fun `every rule can be drawn`() {
        assertTrue("no positions were found at all", examples.size >= 20)
        for ((id, step) in examples) {
            assertTrue("$id rests on no cells, so a hint could not light anything up", step.focusCells.isNotEmpty())
            assertTrue(
                "$id changes nothing, so a hint could not say what it does",
                step.placements.isNotEmpty() || step.eliminations.isNotEmpty(),
            )
            assertTrue(
                "$id points at a cell that is not on the board",
                step.focusCells.all { it in 0 until 81 },
            )
        }
    }

    @Test
    fun `every rule that argues about a house names it`() {
        // A hint that says "in this box" can outline the box. One that cannot has to make do
        // with lighting up cells, which is fine for a chain and wrong for a subset.
        val houseBased = setOf(
            TechniqueId.HIDDEN_SINGLE,
            TechniqueId.LOCKED_CANDIDATES_POINTING,
            TechniqueId.LOCKED_CANDIDATES_CLAIMING,
            TechniqueId.NAKED_PAIR,
            TechniqueId.HIDDEN_PAIR,
            TechniqueId.NAKED_TRIPLE,
            TechniqueId.HIDDEN_TRIPLE,
        )
        for (id in houseBased) {
            val step = examples[id] ?: continue
            assertTrue("$id does not name the house its whole argument is about", step.houses.isNotEmpty())
        }
    }

    @Test
    fun `a hint is fast enough to be a tap, at every grade`() {
        // The budget is one frame. Taken from the shipped batch rather than from freshly
        // generated puzzles, because the grades that matter here are the two hardest and
        // those are exactly the ones a random generator almost never produces.
        val catalog = CatalogReader.from(checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")))
        // Warm the compiler, or the first grade measured pays for all of them.
        repeat(3) { HintEngine.next(GameState.start(catalog.puzzleAt(catalog.indicesOf(Grade.GENTLE).first()))) }

        for (grade in Grade.entries) {
            var worst = 0L
            for (index in catalog.indicesOf(grade).take(10)) {
                val state = GameState.start(catalog.puzzleAt(index))
                val taken = measureNanoTime { HintEngine.next(state) }
                if (taken > worst) worst = taken
            }
            val worstMs = worst / 1_000_000.0
            println("HINT ${grade.displayName} worst ${"%.2f".format(worstMs)} ms")
            assertTrue("a hint on a ${grade.displayName} puzzle took $worstMs ms, and a tap has 16", worstMs < 16.0)
        }
    }
}
