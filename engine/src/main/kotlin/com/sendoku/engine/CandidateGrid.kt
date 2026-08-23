package com.sendoku.engine

/**
 * A board plus the pencil mark set of every empty cell.
 *
 * This is the working surface for every human technique. It is deliberately mutable
 * and incremental: placing a digit strikes it from the peers of that cell, and a
 * technique striking a candidate touches one cell. Nothing recomputes the whole grid.
 *
 * The candidate sets are the source of truth, not a view over the placed digits. A
 * technique eliminates candidates that no placement implies, and that knowledge has
 * to survive, so [candidatesAt] never derives anything on the fly.
 */
public class CandidateGrid private constructor(
    public val dims: Dimensions,
    private val digits: IntArray,
    private val masks: IntArray,
    placedCount: Int,
) {

    /** Side length of the grid. */
    public val size: Int get() = dims.size

    public val cellCount: Int get() = dims.cellCount

    /** Number of cells holding a digit. */
    public var placedCount: Int = placedCount
        private set

    public val emptyCount: Int get() = cellCount - placedCount

    /** True when every cell holds a digit. */
    public val isSolved: Boolean get() = placedCount == cellCount

    /**
     * True when some empty cell has no candidate left, so the grid cannot be completed.
     *
     * A technique solver checks this after every step. It means the givens were wrong or
     * the player made a mistake, not that the technique misfired.
     */
    public val hasContradiction: Boolean
        get() {
            for (index in digits.indices) {
                if (digits[index] == Board.EMPTY && masks[index] == 0) return true
            }
            return false
        }

    private val topology = Topology.of(dims)

    /** Every row, column and box, rows first. */
    public val houses: List<House> get() = topology.houses

    public fun digitAt(index: Int): Int = digits[index]

    public operator fun get(row: Int, col: Int): Int = digits[indexOf(row, col)]

    public fun isEmpty(index: Int): Boolean = digits[index] == Board.EMPTY

    /** The pencil marks of [index]. A cell holding a digit has an empty set. */
    public fun candidatesAt(index: Int): Candidates = Candidates(masks[index])

    public fun candidatesAt(row: Int, col: Int): Candidates = Candidates(masks[indexOf(row, col)])

    /** The cells sharing a row, column or box with [index], excluding [index] itself. */
    public fun peersOf(index: Int): IntArray = topology.peers[index]

    /** The cells of one region, in row-major order. */
    public fun cellsOf(house: House): IntArray = topology.cellsOf(house)

    /** The row, the column and the box that [index] belongs to. */
    public fun housesOf(index: Int): List<House> = topology.housesOfCell[index]

    public fun rowOf(index: Int): Int = index / size

    public fun colOf(index: Int): Int = index % size

    public fun boxOf(index: Int): Int = dims.boxOf(index / size, index % size)

    /**
     * True when the two cells share a row, column or box, so one constrains the other.
     *
     * Computed rather than looked up, because the wings and the chains ask this question
     * far more often than they ask for a whole peer list.
     */
    public fun sees(a: Int, b: Int): Boolean =
        a != b && (rowOf(a) == rowOf(b) || colOf(a) == colOf(b) || boxOf(a) == boxOf(b))

    /** True when [digit] is already placed somewhere in [house]. */
    public fun isPlacedIn(house: House, digit: Int): Boolean =
        topology.cellsOf(house).any { digits[it] == digit }

    public fun indexOf(row: Int, col: Int): Int {
        require(row in 0 until size && col in 0 until size) { "cell ($row, $col) is off the grid" }
        return row * size + col
    }

    /**
     * Writes [digit] into an empty cell and strikes it from every peer.
     *
     * The digit must already be a candidate there. Techniques only ever place digits they
     * proved, so a violation is a bug in the technique rather than a state the caller
     * should handle.
     */
    public fun place(index: Int, digit: Int) {
        require(digits[index] == Board.EMPTY) { "cell $index already holds ${digits[index]}" }
        val bit = Candidates.bitOf(digit)
        require(masks[index] and bit != 0) { "$digit is not a candidate in cell $index" }

        digits[index] = digit
        masks[index] = 0
        placedCount++
        for (peer in topology.peers[index]) {
            masks[peer] = masks[peer] and bit.inv()
        }
    }

    /** Strikes one pencil mark. Returns true when the mark was there to strike. */
    public fun eliminate(index: Int, digit: Int): Boolean {
        val bit = Candidates.bitOf(digit)
        if (masks[index] and bit == 0) return false
        masks[index] = masks[index] and bit.inv()
        return true
    }

    public fun copy(): CandidateGrid =
        CandidateGrid(dims, digits.copyOf(), masks.copyOf(), placedCount)

    /** The placed digits, with the pencil marks dropped. */
    public fun toBoard(): Board = Board.wrap(dims, digits.copyOf())

    /** One line per row, a dot for an empty cell. Meant for tests and logs. */
    override fun toString(): String = toBoard().toString()

    public companion object {

        /**
         * Derives the pencil marks of [board].
         *
         * Throws when the board already repeats a digit in a row, column or box. Use
         * [ofOrNull] when an illegal board is an expected input.
         */
        public fun of(board: Board): CandidateGrid =
            requireNotNull(ofOrNull(board)) { "board repeats a digit in a row, column or box" }

        /** Same as [of], but returns null instead of throwing on an illegal board. */
        public fun ofOrNull(board: Board): CandidateGrid? {
            val dims = board.dims
            val size = dims.size
            val digits = board.toIntArray()
            val rowMask = IntArray(size)
            val colMask = IntArray(size)
            val boxMask = IntArray(size)
            var placed = 0

            for (index in digits.indices) {
                val digit = digits[index]
                if (digit == Board.EMPTY) continue
                val row = index / size
                val col = index % size
                val box = dims.boxOf(row, col)
                val bit = 1 shl (digit - 1)
                if ((rowMask[row] or colMask[col] or boxMask[box]) and bit != 0) return null
                rowMask[row] = rowMask[row] or bit
                colMask[col] = colMask[col] or bit
                boxMask[box] = boxMask[box] or bit
                placed++
            }

            val masks = IntArray(digits.size)
            for (index in digits.indices) {
                if (digits[index] != Board.EMPTY) continue
                val row = index / size
                val col = index % size
                val taken = rowMask[row] or colMask[col] or boxMask[dims.boxOf(row, col)]
                masks[index] = dims.allDigits and taken.inv()
            }

            return CandidateGrid(dims, digits, masks, placed)
        }

    }
}
