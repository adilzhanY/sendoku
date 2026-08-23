package com.sendoku.engine.catalog

import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions

/**
 * Tells whether two puzzles are really the same puzzle wearing a disguise.
 *
 * Relabel every digit, shuffle the three bands of rows, shuffle the rows inside a band,
 * do the same to the columns, and optionally reflect the whole thing across the diagonal.
 * None of that changes a single deduction. The grid looks different and solves identically,
 * which means a player who met one would recognise the other and feel cheated.
 *
 * There are 3,359,232 ways to rearrange a nine by nine grid, times 362,880 ways to relabel
 * it. Comparing two puzzles against all of that head on is hopeless, so this does it in two
 * stages: a [fingerprint] that every one of those transformations leaves alone, and only
 * for puzzles whose fingerprints collide, an exhaustive search with heavy pruning.
 */
public object GridEquivalence {

    /**
     * A cheap value that no symmetry or relabelling can change.
     *
     * Two puzzles with different fingerprints cannot possibly be the same puzzle, which is
     * what makes checking a whole batch affordable. Two with the same fingerprint still
     * usually are not, so a match only means the pair is worth looking at properly.
     */
    public fun fingerprint(board: Board): String {
        val dims = board.dims
        val size = dims.size
        val rowCounts = IntArray(size)
        val colCounts = IntArray(size)
        val boxCounts = IntArray(size)
        val digitCounts = IntArray(size + 1)

        for (index in 0 until dims.cellCount) {
            val digit = board.atIndex(index)
            if (digit == Board.EMPTY) continue
            val row = index / size
            val col = index % size
            rowCounts[row]++
            colCounts[col]++
            boxCounts[dims.boxOf(row, col)]++
            digitCounts[digit]++
        }

        // Rows and columns swap places under a reflection, so they go in together.
        val lines = (rowCounts + colCounts).sorted()
        return buildString {
            append(board.clueCount).append('|')
            append(lines.joinToString(",")).append('|')
            append(boxCounts.sorted().joinToString(",")).append('|')
            append(digitCounts.drop(1).sorted().joinToString(","))
        }
    }

    /**
     * True when some rearrangement and relabelling turns [left] into [right].
     *
     * Exhaustive, and pruned hard: a candidate rearrangement is thrown out the moment its
     * row clue counts fail to line up, long before any digit is looked at.
     */
    public fun areEquivalent(left: Board, right: Board): Boolean {
        val dims = left.dims
        require(dims == right.dims) { "cannot compare a $dims grid with a ${right.dims} one" }
        if (left.clueCount != right.clueCount) return false

        val size = dims.size
        val targetRowCounts = lineCounts(right, byRow = true)
        val targetColCounts = lineCounts(right, byRow = false)
        val rowOrders = orders(dims.boxWidth, dims.boxHeight)
        val colOrders = orders(dims.boxHeight, dims.boxWidth)

        // Reflecting across the diagonal only keeps the boxes the right shape on a square box.
        val forms = if (dims.boxWidth == dims.boxHeight) listOf(left, transpose(left)) else listOf(left)

        for (form in forms) {
            for (rowOrder in rowOrders) {
                var rowsFit = true
                for (row in 0 until size) {
                    if (countIn(form, rowOrder[row], byRow = true) != targetRowCounts[row]) {
                        rowsFit = false
                        break
                    }
                }
                if (!rowsFit) continue

                for (colOrder in colOrders) {
                    var colsFit = true
                    for (col in 0 until size) {
                        if (countIn(form, colOrder[col], byRow = false) != targetColCounts[col]) {
                            colsFit = false
                            break
                        }
                    }
                    if (!colsFit) continue
                    if (matches(form, right, rowOrder, colOrder)) return true
                }
            }
        }
        return false
    }

    /** True when the rearranged [form] becomes [target] under some relabelling of digits. */
    private fun matches(form: Board, target: Board, rowOrder: IntArray, colOrder: IntArray): Boolean {
        val dims = form.dims
        val size = dims.size
        val forward = IntArray(size + 1)
        val backward = IntArray(size + 1)

        for (row in 0 until size) {
            for (col in 0 until size) {
                val from = form[rowOrder[row], colOrder[col]]
                val to = target[row, col]
                if ((from == Board.EMPTY) != (to == Board.EMPTY)) return false
                if (from == Board.EMPTY) continue
                if (forward[from] == 0 && backward[to] == 0) {
                    forward[from] = to
                    backward[to] = from
                } else if (forward[from] != to) {
                    return false
                }
            }
        }
        return true
    }

    private fun lineCounts(board: Board, byRow: Boolean): IntArray =
        IntArray(board.size) { countIn(board, it, byRow) }

    private fun countIn(board: Board, line: Int, byRow: Boolean): Int {
        var count = 0
        for (other in 0 until board.size) {
            val digit = if (byRow) board[line, other] else board[other, line]
            if (digit != Board.EMPTY) count++
        }
        return count
    }

    private fun transpose(board: Board): Board {
        val flipped = Board(board.dims)
        for (row in 0 until board.size) {
            for (col in 0 until board.size) flipped[col, row] = board[row, col]
        }
        return flipped
    }

    /**
     * Every legal reordering of one axis: the groups may swap with each other, and the
     * lines inside a group may swap with each other, but a line never leaves its group.
     */
    private fun orders(groupCount: Int, groupSize: Int): List<IntArray> {
        val groupOrders = permutations(groupCount)
        val innerOrders = permutations(groupSize)
        val result = ArrayList<IntArray>(groupOrders.size * pow(innerOrders.size, groupCount))

        fun build(groups: IntArray, at: Int, chosen: Array<IntArray?>) {
            if (at == groupCount) {
                val order = IntArray(groupCount * groupSize)
                var next = 0
                for (slot in 0 until groupCount) {
                    val base = groups[slot] * groupSize
                    for (inner in chosen[slot]!!) order[next++] = base + inner
                }
                result.add(order)
                return
            }
            for (inner in innerOrders) {
                chosen[at] = inner
                build(groups, at + 1, chosen)
            }
        }

        for (groups in groupOrders) build(groups, 0, arrayOfNulls(groupCount))
        return result
    }

    private fun pow(base: Int, exponent: Int): Int {
        var result = 1
        repeat(exponent) { result *= base }
        return result
    }

    private fun permutations(n: Int): List<IntArray> {
        if (n == 0) return listOf(IntArray(0))
        val result = ArrayList<IntArray>()
        val current = IntArray(n)
        val used = BooleanArray(n)
        fun step(at: Int) {
            if (at == n) {
                result.add(current.copyOf())
                return
            }
            for (value in 0 until n) {
                if (used[value]) continue
                used[value] = true
                current[at] = value
                step(at + 1)
                used[value] = false
            }
        }
        step(0)
        return result
    }
}
