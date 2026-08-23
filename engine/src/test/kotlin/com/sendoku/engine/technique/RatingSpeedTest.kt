package com.sendoku.engine.technique

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import org.junit.jupiter.api.Tag
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Rating has to be fast enough to run on the phone, not just in a batch job.
 *
 * Two places need it. A hint asks the ladder for the next step every time the player taps
 * the button, and the fallback generator rates a freshly made puzzle on the device when
 * the shipped batch runs out. Neither can stall the frame.
 *
 * The budget in the roadmap is 50 ms per rating on a mid range phone. A desktop is several
 * times faster than that, so the bound here is set well under it and the real number is
 * printed, which is what makes a slow regression visible before it ships.
 */
@Tag("slow")
class RatingSpeedTest {

    private val classic = Dimensions.CLASSIC

    private val boards: List<Board> by lazy {
        checkNotNull(javaClass.getResourceAsStream("/rating-corpus.csv"))
            .bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .map { Board.parse(classic, it.substringBefore(',')) }
    }

    @Test
    fun `rating a puzzle stays well inside the phone budget`() {
        val solver = TechniqueSolver()
        // Warm the JIT, or the first few boards measure the compiler rather than the code.
        repeat(2) { for (board in boards) solver.solve(board) }

        var worst = 0L
        val total = measureNanoTime {
            for (board in boards) {
                val one = measureNanoTime { solver.solve(board) }
                if (one > worst) worst = one
            }
        }
        val averageMs = total / boards.size / 1_000_000.0
        val worstMs = worst / 1_000_000.0
        println(
            "RATING average ${"%.2f".format(
                averageMs,
            )} ms, worst ${"%.2f".format(worstMs)} ms over ${boards.size} puzzles",
        )

        assertTrue(averageMs < 10.0, "average rating took $averageMs ms, budget is 10")
        assertTrue(worstMs < 50.0, "worst rating took $worstMs ms, budget is 50")
    }

    @Test
    fun `finding a single hint is fast enough to run on a tap`() {
        val solver = TechniqueSolver()
        repeat(2) { for (board in boards) solver.solve(board) }

        var worst = 0L
        for (board in boards.take(60)) {
            val grid = com.sendoku.engine.CandidateGrid.of(board)
            val one = measureNanoTime { Techniques.ladder.firstNotNullOfOrNull { it.find(grid) } }
            if (one > worst) worst = one
        }
        val worstMs = worst / 1_000_000.0
        println("HINT worst ${"%.2f".format(worstMs)} ms")
        assertTrue(worstMs < 16.0, "a hint took $worstMs ms, which would drop a frame")
    }
}
