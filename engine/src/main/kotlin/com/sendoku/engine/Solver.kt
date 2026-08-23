package com.sendoku.engine

import kotlin.random.Random

/**
 * Backtracking solver over three bitmasks, one per row, column and box.
 *
 * It always fills the empty cell with the fewest candidates first, which is what
 * keeps a 9 by 9 grid well under a millisecond. This solver knows nothing about
 * human technique. It is here to answer two questions: is there a solution, and is
 * there more than one.
 */
public class Solver(private val dims: Dimensions) {

    /**
     * Returns the first solution found, or null when the board cannot be completed.
     *
     * Pass a [random] to shuffle the digit order, which turns this into a random
     * grid filler for the generator.
     */
    public fun solve(board: Board, random: Random? = null): Board? {
        val search = Search(dims, board, random) ?: return null
        search.run(limit = 1)
        return search.firstSolution?.let { Board.wrap(dims, it) }
    }

    /**
     * Counts solutions, stopping as soon as [limit] have been found.
     *
     * The generator only ever needs to know "one or more than one", so the default
     * limit of 2 keeps the hole digging loop cheap.
     */
    public fun countSolutions(board: Board, limit: Int = 2): Int {
        require(limit >= 1) { "limit must be at least 1" }
        val search = Search(dims, board, random = null) ?: return 0
        search.run(limit)
        return search.solutions
    }

    /** True when the board has exactly one completion. */
    public fun hasUniqueSolution(board: Board): Boolean = countSolutions(board, limit = 2) == 1

    /**
     * True when no digit is repeated in any row, column or box. Empty cells are fine,
     * so this checks legality rather than completeness.
     */
    public fun isLegal(board: Board): Boolean = Search(dims, board, random = null) != null

    /**
     * One run of the search, holding its own mutable state so [Solver] itself stays
     * safe to share between threads.
     */
    private class Search private constructor(
        private val dims: Dimensions,
        private val cells: IntArray,
        private val random: Random?,
    ) {
        private val size = dims.size
        private val rowMask = IntArray(size)
        private val colMask = IntArray(size)
        private val boxMask = IntArray(size)

        var solutions: Int = 0
            private set

        var firstSolution: IntArray? = null
            private set

        private var limit = 1

        fun run(limit: Int) {
            this.limit = limit
            solutions = 0
            firstSolution = null
            search()
        }

        /** Returns true when the caller should stop, meaning the limit was reached. */
        private fun search(): Boolean {
            var bestIndex = -1
            var bestMask = 0
            var bestCount = Int.MAX_VALUE

            for (index in cells.indices) {
                if (cells[index] != Board.EMPTY) continue
                val row = index / size
                val col = index % size
                val mask = candidates(row, col)
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
                if (firstSolution == null) firstSolution = cells.copyOf()
                return solutions >= limit
            }

            val row = bestIndex / size
            val col = bestIndex % size
            val box = dims.boxOf(row, col)

            for (digit in digitOrder(bestMask)) {
                val bit = 1 shl (digit - 1)
                cells[bestIndex] = digit
                rowMask[row] = rowMask[row] or bit
                colMask[col] = colMask[col] or bit
                boxMask[box] = boxMask[box] or bit

                val stop = search()

                cells[bestIndex] = Board.EMPTY
                rowMask[row] = rowMask[row] and bit.inv()
                colMask[col] = colMask[col] and bit.inv()
                boxMask[box] = boxMask[box] and bit.inv()

                if (stop) return true
            }
            return false
        }

        private fun candidates(row: Int, col: Int): Int = dims.allDigits and
            rowMask[row].inv() and
            colMask[col].inv() and
            boxMask[dims.boxOf(row, col)].inv()

        private fun digitOrder(mask: Int): IntArray {
            val digits = IntArray(Integer.bitCount(mask))
            var next = 0
            var remaining = mask
            while (remaining != 0) {
                val bit = remaining and -remaining
                digits[next++] = Integer.numberOfTrailingZeros(bit) + 1
                remaining = remaining and bit.inv()
            }
            if (random != null) digits.shuffle(random)
            return digits
        }

        companion object {
            /** Returns null when the starting board already breaks a rule. */
            operator fun invoke(dims: Dimensions, board: Board, random: Random?): Search? {
                val search = Search(dims, board.toIntArray(), random)
                for (index in search.cells.indices) {
                    val digit = search.cells[index]
                    if (digit == Board.EMPTY) continue
                    val row = index / search.size
                    val col = index % search.size
                    val box = dims.boxOf(row, col)
                    val bit = 1 shl (digit - 1)
                    val clash = (search.rowMask[row] or search.colMask[col] or search.boxMask[box]) and bit
                    if (clash != 0) return null
                    search.rowMask[row] = search.rowMask[row] or bit
                    search.colMask[col] = search.colMask[col] or bit
                    search.boxMask[box] = search.boxMask[box] or bit
                }
                return search
            }
        }
    }
}

/** Fisher-Yates, because IntArray has no shuffle that takes a [Random]. */
private fun IntArray.shuffle(random: Random) {
    for (i in size - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}
