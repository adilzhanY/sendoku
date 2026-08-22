package com.sendoku.engine

/**
 * A grid of digits. Zero means empty.
 *
 * The board carries no notion of which cells were given and which the player typed.
 * That belongs to the game layer, not to the engine.
 */
public class Board private constructor(
    public val dims: Dimensions,
    private val cells: IntArray,
) {

    public constructor(dims: Dimensions) : this(dims, IntArray(dims.cellCount))

    /** Side length of the grid. */
    public val size: Int get() = dims.size

    /** Number of filled cells. */
    public val clueCount: Int get() = cells.count { it != EMPTY }

    /** Number of empty cells. */
    public val emptyCount: Int get() = cells.size - clueCount

    /** True when no cell is empty. Says nothing about whether the grid is legal. */
    public val isFull: Boolean get() = cells.none { it == EMPTY }

    public operator fun get(row: Int, col: Int): Int = cells[index(row, col)]

    public operator fun set(row: Int, col: Int, value: Int) {
        require(value in 0..size) { "digit $value is out of range for a $size by $size grid" }
        cells[index(row, col)] = value
    }

    public fun atIndex(index: Int): Int = cells[index]

    public fun setAtIndex(index: Int, value: Int) {
        require(value in 0..size) { "digit $value is out of range for a $size by $size grid" }
        cells[index] = value
    }

    public fun copy(): Board = Board(dims, cells.copyOf())

    /** A defensive copy of the raw cells, in row-major order. */
    public fun toIntArray(): IntArray = cells.copyOf()

    private fun index(row: Int, col: Int): Int {
        require(row in 0 until size && col in 0 until size) { "cell ($row, $col) is off the grid" }
        return row * size + col
    }

    override fun equals(other: Any?): Boolean =
        other is Board && other.dims == dims && other.cells.contentEquals(cells)

    override fun hashCode(): Int = 31 * dims.hashCode() + cells.contentHashCode()

    /** One line per row, a dot for an empty cell. Meant for tests and logs. */
    override fun toString(): String = buildString {
        for (row in 0 until size) {
            for (col in 0 until size) {
                append(Digits.toChar(get(row, col)))
            }
            if (row < size - 1) append('\n')
        }
    }

    public companion object {
        /** The value of an empty cell. */
        public const val EMPTY: Int = 0

        /**
         * Reads a board from text. Digits are 1 to 9 then A to G, and any of
         * `.`, `0`, `-` or a space means empty. Line breaks are ignored, so both a
         * single 81 character line and a nine line block work.
         */
        public fun parse(dims: Dimensions, text: String): Board {
            val board = Board(dims)
            var index = 0
            for (char in text) {
                if (char == '\n' || char == '\r') continue
                require(index < dims.cellCount) { "text holds more than ${dims.cellCount} cells" }
                board.cells[index++] = Digits.fromChar(char, dims.size)
            }
            require(index == dims.cellCount) { "text holds $index cells, expected ${dims.cellCount}" }
            return board
        }

        /** Wraps [cells] without copying. Internal callers own the array. */
        internal fun wrap(dims: Dimensions, cells: IntArray): Board {
            require(cells.size == dims.cellCount) { "expected ${dims.cellCount} cells, got ${cells.size}" }
            return Board(dims, cells)
        }
    }
}

/** Digit to character mapping, so 16 by 16 grids stay readable as text. */
public object Digits {

    private const val ALPHABET = "123456789ABCDEFG"

    public fun toChar(digit: Int): Char =
        if (digit == Board.EMPTY) '.' else ALPHABET[digit - 1]

    public fun fromChar(char: Char, size: Int): Int {
        if (char == '.' || char == '0' || char == '-' || char == ' ') return Board.EMPTY
        val digit = ALPHABET.indexOf(char.uppercaseChar()) + 1
        require(digit in 1..size) { "'$char' is not a digit in a $size by $size grid" }
        return digit
    }
}
