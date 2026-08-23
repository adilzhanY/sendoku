package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.House

/**
 * Two digits with only the same two homes left in a house own those two cells, so every
 * other candidate in them can go.
 */
public object HiddenPair : Technique by HiddenSubset(TechniqueId.HIDDEN_PAIR, size = 2)

/** Three digits confined to the same three cells own them. */
public object HiddenTriple : Technique by HiddenSubset(TechniqueId.HIDDEN_TRIPLE, size = 3)

/** Four digits confined to the same four cells own them. */
public object HiddenQuad : Technique by HiddenSubset(TechniqueId.HIDDEN_QUAD, size = 4)

/**
 * The shared rule behind every hidden subset, and the exact mirror of [NakedSubset].
 *
 * A naked subset starts from cells and counts digits. A hidden subset starts from digits
 * and counts cells: `n` digits that can only live in the same `n` cells have filled them
 * between them, so anything else in those cells is dead.
 *
 * Hidden subsets are much harder for a player to spot than naked ones, because the
 * evidence is spread across the house rather than sitting in the cells themselves. That
 * is why they sit higher on the ladder despite being the same argument.
 */
internal class HiddenSubset(override val id: TechniqueId, private val size: Int) : Technique {

    init {
        require(size >= 2) { "a hidden subset needs at least two digits" }
    }

    override fun find(grid: CandidateGrid): Deduction? {
        for (house in grid.houses) {
            val deduction = findIn(grid, house)
            if (deduction != null) return deduction
        }
        return null
    }

    private fun findIn(grid: CandidateGrid, house: House): Deduction? {
        val cells = grid.cellsOf(house)
        val digits = ArrayList<Int>(grid.size)
        val homes = ArrayList<List<Int>>(grid.size)

        for (digit in 1..grid.size) {
            if (grid.isPlacedIn(house, digit)) continue
            val where = cells.filter { digit in grid.candidatesAt(it) }
            // One home is a hidden single, which a cheaper rule already handles. More homes
            // than the subset is too many to be locked by it.
            if (where.size !in 2..size) continue
            digits.add(digit)
            homes.add(where)
        }
        if (digits.size < size) return null

        var found: Deduction? = null
        forEachCombination(digits.size, size) { picks ->
            val chosen = Candidates.of(*IntArray(size) { digits[picks[it]] })
            val owned = sortedSetOf<Int>()
            for (pick in picks) owned.addAll(homes[pick])
            if (owned.size != size) return@forEachCombination false

            val eliminations = buildList {
                for (cell in owned) {
                    (grid.candidatesAt(cell) without chosen).forEach { add(CellDigit(cell, it)) }
                }
            }
            if (eliminations.isEmpty()) return@forEachCombination false

            found = Deduction(
                technique = id,
                focusCells = owned.toList(),
                focusCandidates = owned.flatMap { cell ->
                    (grid.candidatesAt(cell) and chosen).toList().map { CellDigit(cell, it) }
                },
                houses = listOf(house),
                eliminations = eliminations,
            )
            true
        }
        return found
    }
}
