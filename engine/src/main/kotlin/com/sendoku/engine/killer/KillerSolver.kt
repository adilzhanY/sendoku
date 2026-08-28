package com.sendoku.engine.killer

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions

/**
 * Backtracking over a Killer grid, for the one question a generator needs answering: is this
 * set of cages describing exactly one grid, or more than one.
 *
 * The same shape as the ordinary solver, three bitmasks for row, column and box, with the
 * cage constraint added as a fourth. It knows nothing about human technique, which is a
 * separate ladder and the expensive half of Killer.
 *
 * Two prunings do the work. Neither is clever and both are worth their weight:
 *
 *  - A digit already used in the cage is out, since a cage never repeats.
 *  - What is left of a cage has to be reachable. If four cells remain and they need thirty
 *    more, the largest four distinct digits have to cover thirty, and if the cage needs five
 *    across four cells the smallest four have to fit under five. Without this the search
 *    walks a long way down cages that were arithmetically dead at the first cell.
 */
public class KillerSolver(private val dims: Dimensions, private val puzzle: KillerPuzzle) {

    private val size = dims.size

    /** Counts solutions, stopping at [limit]. The generator only ever asks for two. */
    public fun countSolutions(limit: Int = 2): Int {
        require(limit >= 1) { "limit must be at least 1" }
        return Search(dims, puzzle.cages, puzzle.cageOfCell).run(limit)
    }

    public fun hasUniqueSolution(): Boolean = countSolutions(limit = 2) == 1

    /** The first grid found, or null when the cages describe nothing at all. */
    public fun solve(): Board? = Search(dims, puzzle.cages, puzzle.cageOfCell).first()

    public companion object {

        /**
         * The grid a set of cages describes, without being told the answer first.
         *
         * [KillerPuzzle] cannot be built without a solution, because it checks that every
         * cage's sum matches it, which is the right check everywhere except here: a layout
         * that has just been drawn, or one written down in a lesson, is exactly the case
         * where the answer is the thing being asked for.
         */
        public fun solve(dims: Dimensions, cages: List<Cage>): Board? =
            Search(dims, cages, ownerMap(dims, cages)).first()

        /** How many grids a set of cages describes, stopping at [limit]. */
        public fun countSolutions(dims: Dimensions, cages: List<Cage>, limit: Int = 2): Int =
            Search(dims, cages, ownerMap(dims, cages)).run(limit)

        private fun ownerMap(dims: Dimensions, cages: List<Cage>): IntArray = IntArray(dims.cellCount).also { map ->
            for ((index, cage) in cages.withIndex()) {
                for (cell in cage.cells) map[cell] = index
            }
        }
    }

    private class Search(
        private val dims: Dimensions,
        private val cages: List<Cage>,
        private val cageOfCell: IntArray,
    ) {
        private val size = dims.size
        private val cells = IntArray(dims.cellCount)
        private val rowMask = IntArray(size)
        private val colMask = IntArray(size)
        private val boxMask = IntArray(size)

        /** Per cage: which digits it holds, how much is left to reach, how many cells are unfilled. */
        private val cageMask = IntArray(cages.size)
        private val cageLeft = IntArray(cages.size) { cages[it].sum }
        private val cageEmpty = IntArray(cages.size) { cages[it].size }

        private var solutions = 0
        private var found: IntArray? = null

        fun run(limit: Int): Int {
            search(limit)
            return solutions
        }

        fun first(): Board? {
            search(1)
            return found?.let { Board.wrap(dims, it) }
        }

        private fun search(limit: Int): Boolean {
            var bestIndex = -1
            var bestMask = 0
            var bestCount = Int.MAX_VALUE

            for (index in cells.indices) {
                if (cells[index] != Board.EMPTY) continue
                val mask = candidates(index)
                val count = Integer.bitCount(mask)
                if (count == 0) return false
                if (count < bestCount) {
                    bestCount = count
                    bestIndex = index
                    bestMask = mask
                    if (count == 1) break
                }
            }

            if (bestIndex == -1) {
                solutions++
                if (found == null) found = cells.copyOf()
                return solutions >= limit
            }

            val row = bestIndex / size
            val col = bestIndex % size
            val box = dims.boxOf(row, col)
            val cage = cageOfCell[bestIndex]

            var remaining = bestMask
            while (remaining != 0) {
                val bit = remaining and -remaining
                remaining = remaining and bit.inv()
                val digit = Integer.numberOfTrailingZeros(bit) + 1

                cells[bestIndex] = digit
                rowMask[row] = rowMask[row] or bit
                colMask[col] = colMask[col] or bit
                boxMask[box] = boxMask[box] or bit
                cageMask[cage] = cageMask[cage] or bit
                cageLeft[cage] -= digit
                cageEmpty[cage]--

                val stop = if (reachable(cage)) search(limit) else false

                cells[bestIndex] = Board.EMPTY
                rowMask[row] = rowMask[row] and bit.inv()
                colMask[col] = colMask[col] and bit.inv()
                boxMask[box] = boxMask[box] and bit.inv()
                cageMask[cage] = cageMask[cage] and bit.inv()
                cageLeft[cage] += digit
                cageEmpty[cage]++

                if (stop) return true
            }
            return false
        }

        private fun candidates(index: Int): Int {
            val row = index / size
            val col = index % size
            return dims.allDigits and
                rowMask[row].inv() and
                colMask[col].inv() and
                boxMask[dims.boxOf(row, col)].inv() and
                cageMask[cageOfCell[index]].inv()
        }

        /** Can what is left of this cage still add up to what it owes? */
        private fun reachable(cage: Int): Boolean {
            val left = cageLeft[cage]
            val empty = cageEmpty[cage]
            if (empty == 0) return left == 0
            if (left <= 0) return false

            // The digits this cage has not used yet, largest and smallest [empty] of them.
            val available = dims.allDigits and cageMask[cage].inv()
            var most = 0
            var least = 0
            var taken = 0
            var digit = size
            while (digit >= 1 && taken < empty) {
                if (available and (1 shl (digit - 1)) != 0) {
                    most += digit
                    taken++
                }
                digit--
            }
            if (taken < empty) return false
            taken = 0
            digit = 1
            while (digit <= size && taken < empty) {
                if (available and (1 shl (digit - 1)) != 0) {
                    least += digit
                    taken++
                }
                digit++
            }
            return left in least..most
        }
    }
}
