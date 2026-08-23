package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.House

/**
 * A digit with only one home left in a row, column or box must go there.
 *
 * Where a naked single looks at one cell and asks what it can hold, this looks at one
 * region and asks where a digit can live. That is why it finds placements a naked
 * single cannot: the cell often still has several candidates of its own.
 */
public object HiddenSingle : Technique {

    override val id: TechniqueId get() = TechniqueId.HIDDEN_SINGLE

    override fun find(grid: CandidateGrid): Deduction? {
        for (house in grid.houses) {
            val deduction = findIn(grid, house)
            if (deduction != null) return deduction
        }
        return null
    }

    private fun findIn(grid: CandidateGrid, house: House): Deduction? {
        val cells = grid.cellsOf(house)
        for (digit in 1..grid.size) {
            if (grid.isPlacedIn(house, digit)) continue
            var home = -1
            var homes = 0
            for (cell in cells) {
                if (digit !in grid.candidatesAt(cell)) continue
                home = cell
                if (++homes > 1) break
            }
            if (homes != 1) continue
            return Deduction(
                technique = id,
                focusCells = listOf(home),
                focusCandidates = listOf(CellDigit(home, digit)),
                houses = listOf(house),
                placements = listOf(CellDigit(home, digit)),
            )
        }
        return null
    }
}
