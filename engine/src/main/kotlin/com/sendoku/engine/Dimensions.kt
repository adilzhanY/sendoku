package com.sendoku.engine

/**
 * The shape of a puzzle.
 *
 * A box is [boxWidth] cells wide and [boxHeight] cells tall, which makes the whole
 * grid `boxWidth * boxHeight` cells on a side. Classic sudoku is 3 by 3. A 6 by 6
 * kids' grid is 3 by 2. Hexadoku is 4 by 4.
 *
 * Everything in the engine takes its size from here, so a new grid size costs a
 * constructor call and nothing else.
 */
public data class Dimensions(val boxWidth: Int, val boxHeight: Int) {

    init {
        require(boxWidth >= 1 && boxHeight >= 1) { "box sides must be positive" }
        require(boxWidth * boxHeight <= MAX_SIZE) {
            "grids wider than $MAX_SIZE are not supported, digits are held in an Int bitmask"
        }
    }

    /** Side length of the grid, which is also the largest digit. */
    public val size: Int get() = boxWidth * boxHeight

    /** Total number of cells. */
    public val cellCount: Int get() = size * size

    /** A bitmask with every legal digit set, used as the starting candidate set. */
    public val allDigits: Int get() = (1 shl size) - 1

    /** Index of the box that contains ([row], [col]). */
    public fun boxOf(row: Int, col: Int): Int = (row / boxHeight) * boxHeight + (col / boxWidth)

    public companion object {
        /** Digits live in the bits of an Int, so 16 is the ceiling. */
        public const val MAX_SIZE: Int = 16

        /** 4 by 4, for children and for fast tests. */
        public val JUNIOR: Dimensions = Dimensions(2, 2)

        /** 6 by 6, boxes are 3 wide and 2 tall. */
        public val SIX: Dimensions = Dimensions(3, 2)

        /** 9 by 9, ordinary sudoku. */
        public val CLASSIC: Dimensions = Dimensions(3, 3)

        /** 16 by 16, digits 1 to 16 shown as 1 to 9 then A to G. */
        public val HEXADOKU: Dimensions = Dimensions(4, 4)
    }
}
