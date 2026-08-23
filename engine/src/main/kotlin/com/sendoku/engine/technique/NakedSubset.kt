package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.House

/**
 * Two cells in a house holding the same two candidates take those two digits between
 * them, so no other cell in that house can hold either.
 */
public object NakedPair : Technique by NakedSubset(TechniqueId.NAKED_PAIR, size = 2)

/**
 * Three cells in a house sharing three candidates between them take those three digits,
 * so no other cell in that house can hold any of them.
 *
 * The three cells do not each need all three digits. Two of them holding a pair and one
 * holding a third digit still locks the set, which is what makes a triple harder to spot
 * than a pair.
 */
public object NakedTriple : Technique by NakedSubset(TechniqueId.NAKED_TRIPLE, size = 3)

/**
 * Four cells in a house sharing four candidates between them take those four digits.
 *
 * The rarest of the naked subsets and the hardest to see, because the four cells need
 * not share a single digit in common.
 */
public object NakedQuad : Technique by NakedSubset(TechniqueId.NAKED_QUAD, size = 4)

/**
 * The shared rule behind every naked subset.
 *
 * `n` cells in one house whose candidates come to exactly `n` digits have claimed those
 * digits. Pair, triple and quad are the same argument at different sizes, so they are
 * the same code at different sizes.
 */
internal class NakedSubset(
    override val id: TechniqueId,
    private val size: Int,
) : Technique {

    init {
        require(size >= 2) { "a naked subset needs at least two cells" }
    }

    override fun find(grid: CandidateGrid): Deduction? {
        for (house in grid.houses) {
            val deduction = findIn(grid, house)
            if (deduction != null) return deduction
        }
        return null
    }

    private fun findIn(grid: CandidateGrid, house: House): Deduction? {
        // A cell with more candidates than the subset is too big to be part of it, and a
        // cell with one candidate is a naked single that a cheaper rule already handles.
        val cells = grid.cellsOf(house).filter { grid.candidatesAt(it).size in 2..size }
        if (cells.size < size) return null

        var found: Deduction? = null
        forEachCombination(cells.size, size) { picks ->
            val subset = picks.map { cells[it] }
            var union = Candidates.EMPTY
            for (cell in subset) union = union or grid.candidatesAt(cell)
            if (union.size != size) return@forEachCombination false

            val eliminations = buildList {
                for (cell in grid.cellsOf(house)) {
                    if (cell in subset) continue
                    val shared = grid.candidatesAt(cell) and union
                    shared.forEach { add(CellDigit(cell, it)) }
                }
            }
            if (eliminations.isEmpty()) return@forEachCombination false

            found = Deduction(
                technique = id,
                focusCells = subset,
                focusCandidates = subset.flatMap { cell ->
                    grid.candidatesAt(cell).toList().map { CellDigit(cell, it) }
                },
                houses = listOf(house),
                eliminations = eliminations,
            )
            true
        }
        return found
    }
}
