package com.sendoku.engine.killer

import com.sendoku.engine.Dimensions
import org.junit.jupiter.api.Tag
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * How long a Killer takes to make, at each cage size.
 *
 * The number the batch job needs. Cages of two and three are quick; every extra cell in the
 * range means fewer sums across the grid, more layouts that allow a second solution, and more
 * redraws before one sticks.
 *
 * Combination pruning inside the search was tried here and taken out again. Asking the exact
 * question at every node, which digits can still make up what a cage owes, cost more in the
 * asking than the reachability check it replaced saved in the searching: the cage tests went
 * from three seconds to a minute. The combination tables stayed, because the techniques above
 * the solver do need them, and they are wanted once per deduction rather than once per node.
 *
 * The numbers are printed rather than asserted tightly. What is asserted is only that a batch
 * is possible at all: a size range that cannot produce a puzzle is one no batch job can use.
 */
@Tag("slow")
class KillerSpeedTest {

    @Test
    fun `bigger cages are now within reach`() {
        for (top in 3..4) {
            var made = 0
            val millis = measureTimeMillis {
                for (seed in 1..3) {
                    if (KillerGenerator(Dimensions.CLASSIC, Random(seed), sizes = 2..top).next() != null) made++
                }
            }
            println("KILLER sizes 2..$top made $made of 3 in ${millis}ms")
            assertTrue(made > 0, "no puzzle at all with cages up to $top")
        }
    }
}
