package com.sendoku.engine

import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.technique.TechniqueSolver
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The engine suite has to stay fast enough to run on every keystroke.
 *
 * The whole reason the engine has no Android in it is that its tests come back in seconds
 * rather than waiting on an emulator. That property is easy to lose one slow test at a time
 * and hard to get back, so the two things that would cost it are measured here directly.
 */
@Tag("slow")
class SuiteSpeedTest {

    @Test
    fun `rating a puzzle stays in the low milliseconds`() {
        val solver = TechniqueSolver()
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(4242))
        val puzzles = buildList { while (size < 30) maker.next()?.let { add(it.puzzle.givens) } }
        repeat(2) { for (board in puzzles) solver.solve(board) }

        val elapsed = measureTimeMillis { for (board in puzzles) solver.solve(board) }
        val each = elapsed.toDouble() / puzzles.size
        println("SPEED rating ${"%.2f".format(each)} ms per puzzle")
        assertTrue(each < 20.0, "rating took $each ms per puzzle")
    }

    @Test
    fun `generating a puzzle stays in the low milliseconds`() {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(4243))
        repeat(5) { maker.next() }
        val elapsed = measureTimeMillis { repeat(30) { maker.next() } }
        val each = elapsed / 30.0
        println("SPEED generating ${"%.2f".format(each)} ms per puzzle")
        assertTrue(each < 40.0, "generating took $each ms per puzzle")
    }
}
