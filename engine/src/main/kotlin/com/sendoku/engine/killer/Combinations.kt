package com.sendoku.engine.killer

import com.sendoku.engine.Candidates

/**
 * Which sets of digits can add up to a cage's sum.
 *
 * Every cage technique rests on this one question: a cage of four cells adding to eleven can
 * only be 1234, so no cell in it can hold a 5. That single fact, applied everywhere, is most
 * of what solving a Killer by hand actually is.
 *
 * The answers are worked out once and kept. There are only a few thousand of them for a nine
 * by nine grid and they never change, so recomputing them inside a solver loop would be the
 * one piece of arithmetic in this engine that is genuinely wasteful.
 *
 * A cage never repeats a digit, so every combination is a set rather than a multiset. That is
 * the ordinary Killer rule and it is what makes the sets small enough to enumerate at all.
 */
public object Combinations {

    /**
     * Every set of [size] distinct digits from 1 to [top] adding to [sum], as bitmasks.
     *
     * Empty when the sum is impossible, which is a useful answer rather than an error: a
     * cage whose remaining cells cannot add up to what is left has been broken by a wrong
     * digit somewhere, and the solver wants to hear that as "no combinations".
     */
    public fun of(size: Int, sum: Int, top: Int = DEFAULT_TOP): List<Int> =
        cache.getOrPut(Key(size, sum, top)) { build(size, sum, top) }

    /**
     * Every digit that appears in at least one combination, as a bitmask.
     *
     * This is the useful form for a technique: any digit outside it cannot appear anywhere
     * in the cage, whatever else is true of the board.
     */
    public fun possibleDigits(size: Int, sum: Int, top: Int = DEFAULT_TOP): Int =
        of(size, sum, top).fold(0) { mask, combination -> mask or combination }

    /**
     * The combinations that are still possible given what each cell can hold.
     *
     * [allowed] is one candidate mask per cell of the cage. A combination survives when its
     * digits can be dealt out to the cells one each, which is a small matching problem and
     * is done here by brute force: a cage is at most nine cells and usually four.
     */
    public fun fitting(sum: Int, allowed: List<Int>, top: Int = DEFAULT_TOP): List<Int> =
        of(allowed.size, sum, top).filter { canDealOut(it, allowed) }

    /** Whether the digits in [combination] can be given out one to each cell in [allowed]. */
    private fun canDealOut(combination: Int, allowed: List<Int>): Boolean {
        val digits = Candidates(combination).toList()
        if (digits.size != allowed.size) return false
        return match(digits, allowed, 0, BooleanArray(allowed.size))
    }

    /**
     * Hungarian by hand: try each unused cell for the next digit.
     *
     * A cage is nine cells at the very most and four in almost every puzzle, so the worst
     * case here is small enough that a real matching algorithm would be more code for less
     * clarity and no measurable difference.
     */
    private fun match(digits: List<Int>, allowed: List<Int>, at: Int, used: BooleanArray): Boolean {
        if (at == digits.size) return true
        val bit = 1 shl (digits[at] - 1)
        for (cell in allowed.indices) {
            if (used[cell] || allowed[cell] and bit == 0) continue
            used[cell] = true
            if (match(digits, allowed, at + 1, used)) return true
            used[cell] = false
        }
        return false
    }

    private fun build(size: Int, sum: Int, top: Int): List<Int> {
        if (size < 1 || size > top) return emptyList()
        val found = ArrayList<Int>()
        fun walk(from: Int, left: Int, remaining: Int, mask: Int) {
            if (left == 0) {
                if (remaining == 0) found.add(mask)
                return
            }
            for (digit in from..top) {
                if (digit > remaining) break
                walk(digit + 1, left - 1, remaining - digit, mask or (1 shl (digit - 1)))
            }
        }
        walk(1, size, sum, 0)
        return found
    }

    private data class Key(val size: Int, val sum: Int, val top: Int)

    private val cache = HashMap<Key, List<Int>>()

    private const val DEFAULT_TOP = 9
}
