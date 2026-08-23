package com.sendoku.engine.technique

/**
 * Walks every combination of [choose] indices out of [count], smallest first, and stops
 * as soon as [action] returns true.
 *
 * Written by hand because the sizes are tiny and never grow: a subset or a fish is at
 * most four out of sixteen. Building a list of lists per house would cost more than the
 * technique that consumes it.
 *
 * The array handed to [action] is reused between calls, so read it, do not keep it.
 */
internal inline fun forEachCombination(count: Int, choose: Int, action: (IntArray) -> Boolean) {
    if (choose > count || choose <= 0) return
    val picks = IntArray(choose) { it }
    while (true) {
        if (action(picks)) return
        var slot = choose - 1
        while (slot >= 0 && picks[slot] == count - choose + slot) slot--
        if (slot < 0) return
        picks[slot]++
        for (next in slot + 1 until choose) picks[next] = picks[next - 1] + 1
    }
}
