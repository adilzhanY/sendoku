package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Solver
import kotlin.random.Random

/**
 * Makes a Killer puzzle by solving an empty grid and then drawing cages over the answer.
 *
 * The cages come first and the difficulty comes second, which is the opposite way round from
 * the classic generator. There are no clues to remove: every Killer grid starts empty, and
 * what makes one harder than another is the shape and the arithmetic of its cages. Small
 * cages give a lot away, a cage of one is a given digit written differently, so the size
 * distribution is the main dial.
 *
 * Uniqueness is checked the same way as always, by counting solutions and stopping at two.
 * A cage layout that allows two grids is thrown away and redrawn rather than patched: patching
 * means merging cages, which changes the shape everywhere and has to be rechecked anyway.
 */
public class KillerGenerator(
    private val dims: Dimensions = Dimensions.CLASSIC,
    private val random: Random = Random.Default,
    /**
     * Cage sizes to aim for.
     *
     * Ones are excluded on purpose: a single cell is a given digit written differently. The
     * top of the range matters more than it looks. Every extra cell in a cage is one fewer
     * sum across the grid, so layouts of twos and threes are usually unique on the first or
     * second try while layouts reaching four often need dozens of redraws: measured over five
     * seeds, two to three took between thirty and two hundred and eighty milliseconds and two
     * to four took between eight hundred and thirteen thousand.
     *
     * Bigger cages are what makes a hard Killer hard, so this will have to rise. What it
     * needs first is the pruning a real Killer solver has, enumerating the digit combinations
     * a cage sum allows rather than checking only that the remainder is reachable, and that
     * belongs with the technique ladder rather than here.
     */
    private val sizes: IntRange = 2..3,
) {

    private val solver = Solver(dims)

    /**
     * Returns a puzzle with exactly one solution, or null if [attempts] layouts all failed.
     *
     * Null rather than looping forever, so a caller generating a batch can move on. At the
     * default sizes most layouts are unique first time.
     */
    public fun next(attempts: Int = 40): KillerPuzzle? {
        val solution = solver.solve(Board(dims), random) ?: return null
        repeat(attempts) {
            val cages = drawCages(solution)
            val puzzle = KillerPuzzle(dims, cages, solution)
            if (KillerSolver(dims, puzzle).hasUniqueSolution()) return puzzle
        }
        return null
    }

    /**
     * Grows cages out of unclaimed cells until every cell belongs to one.
     *
     * Growth is by orthogonal neighbour, so a cage is always a connected blob rather than a
     * scattering, which is what a player expects to see drawn on the grid. A cage stops early
     * when it runs out of neighbours that are both free and not already holding its digits,
     * and that is why the sizes are a target rather than a promise.
     */
    private fun drawCages(solution: Board): List<Cage> {
        val size = dims.size
        val owner = IntArray(dims.cellCount) { UNCLAIMED }
        val cages = mutableListOf<Cage>()

        val order = (0 until dims.cellCount).shuffled(random)
        for (start in order) {
            if (owner[start] != UNCLAIMED) continue
            val target = sizes.random(random)
            val members = mutableListOf(start)
            var digits = 1 shl (solution.atIndex(start) - 1)
            owner[start] = cages.size

            while (members.size < target) {
                val candidates = members
                    .flatMap { neighboursOf(it, size) }
                    .filter { owner[it] == UNCLAIMED }
                    .filter { digits and (1 shl (solution.atIndex(it) - 1)) == 0 }
                    .distinct()
                if (candidates.isEmpty()) break
                val next = candidates.random(random)
                owner[next] = cages.size
                digits = digits or (1 shl (solution.atIndex(next) - 1))
                members.add(next)
            }

            cages.add(Cage(members.sumOf { solution.atIndex(it) }, members.sorted()))
        }
        return cages
    }

    private fun neighboursOf(cell: Int, size: Int): List<Int> {
        val row = cell / size
        val col = cell % size
        return buildList {
            if (row > 0) add(cell - size)
            if (row < size - 1) add(cell + size)
            if (col > 0) add(cell - 1)
            if (col < size - 1) add(cell + 1)
        }
    }

    private companion object {
        const val UNCLAIMED = -1
    }
}
