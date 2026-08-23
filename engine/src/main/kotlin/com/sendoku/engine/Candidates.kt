package com.sendoku.engine

/**
 * A set of digits held in the bits of an Int. Digit `d` lives in bit `d - 1`.
 *
 * This is what a pencil mark set is, both for the player and for every technique in
 * the solver. Wrapping the Int costs nothing at runtime because the class is inline,
 * so the hot paths still shift and mask raw integers.
 *
 * The set carries no notion of grid size. A [Dimensions] is only needed to build a
 * full set, see [Candidates.all].
 */
@JvmInline
public value class Candidates(public val mask: Int) {

    /** How many digits are in the set. */
    public val size: Int get() = Integer.bitCount(mask)

    public val isEmpty: Boolean get() = mask == 0

    public val isNotEmpty: Boolean get() = mask != 0

    /** True when the set holds exactly one digit, which is what makes a naked single. */
    public val isSingle: Boolean get() = mask != 0 && mask and (mask - 1) == 0

    /** The only digit in the set, or [Board.EMPTY] when the set does not hold exactly one. */
    public val single: Int get() = if (isSingle) Integer.numberOfTrailingZeros(mask) + 1 else Board.EMPTY

    /** The smallest digit in the set, or [Board.EMPTY] when the set is empty. */
    public val lowest: Int
        get() = if (mask == 0) Board.EMPTY else Integer.numberOfTrailingZeros(mask) + 1

    public operator fun contains(digit: Int): Boolean = mask and bitOf(digit) != 0

    public operator fun plus(digit: Int): Candidates = Candidates(mask or bitOf(digit))

    public operator fun minus(digit: Int): Candidates = Candidates(mask and bitOf(digit).inv())

    /** Digits in both sets. */
    public infix fun and(other: Candidates): Candidates = Candidates(mask and other.mask)

    /** Digits in either set. */
    public infix fun or(other: Candidates): Candidates = Candidates(mask or other.mask)

    /** Digits in this set that are not in [other]. */
    public infix fun without(other: Candidates): Candidates = Candidates(mask and other.mask.inv())

    /** True when the two sets share at least one digit. */
    public infix fun overlaps(other: Candidates): Boolean = mask and other.mask != 0

    /** True when every digit of [other] is also here. An empty [other] is always contained. */
    public fun containsAll(other: Candidates): Boolean = mask and other.mask == other.mask

    /** Walks the digits from smallest to largest. */
    public inline fun forEach(action: (Int) -> Unit) {
        var remaining = mask
        while (remaining != 0) {
            val bit = remaining and -remaining
            action(Integer.numberOfTrailingZeros(bit) + 1)
            remaining = remaining and bit.inv()
        }
    }

    /** The digits, smallest first. Allocates, so keep it out of the solver's inner loops. */
    public fun toList(): List<Int> {
        val digits = ArrayList<Int>(size)
        forEach { digits.add(it) }
        return digits
    }

    /** Renders as `{1,4,7}`, so a failing test says what it actually saw. */
    override fun toString(): String = toList().joinToString(prefix = "{", postfix = "}", separator = ",")

    public companion object {
        /** The set with nothing in it. */
        public val EMPTY: Candidates = Candidates(0)

        public fun of(vararg digits: Int): Candidates {
            var mask = 0
            for (digit in digits) mask = mask or bitOf(digit)
            return Candidates(mask)
        }

        /** Every digit legal in a grid of this shape, which is where a cell starts. */
        public fun all(dims: Dimensions): Candidates = Candidates(dims.allDigits)

        /** The single bit that stands for [digit]. */
        public fun bitOf(digit: Int): Int {
            require(digit in 1..Dimensions.MAX_SIZE) { "digit $digit is not a digit" }
            return 1 shl (digit - 1)
        }
    }
}
