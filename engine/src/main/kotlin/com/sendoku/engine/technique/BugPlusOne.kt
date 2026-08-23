package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * BUG plus one. Bivalue universal grave, with a single cell that escapes it.
 *
 * A grid where every unsolved cell holds exactly two candidates and every digit appears
 * exactly twice in every house is a grave: it always has an even number of solutions, so
 * it can never be the state of a proper puzzle. If the grid is one extra candidate away
 * from that, the extra candidate is the only thing keeping the puzzle alive, so it has to
 * be true.
 *
 * Like [UniqueRectangle] this reasons about the puzzle rather than the grid, and it is
 * only sound because the solution is unique. It also happens to be the cheapest rule on
 * the whole ladder to check, since it is a single sweep.
 */
public object BugPlusOne : Technique {

    override val id: TechniqueId get() = TechniqueId.BUG_PLUS_ONE

    override fun find(grid: CandidateGrid): Deduction? {
        var extraCell = -1
        for (cell in 0 until grid.cellCount) {
            if (!grid.isEmpty(cell)) continue
            when (grid.candidatesAt(cell).size) {
                2 -> Unit
                3 -> {
                    if (extraCell != -1) return null
                    extraCell = cell
                }
                else -> return null
            }
        }
        if (extraCell == -1) return null

        // Every house must be in the grave state, bar the odd digit in the odd cell's own
        // three houses. Checking this rather than trusting the shape is what keeps the rule
        // honest: a near miss would place a digit that is simply wrong.
        val odd = oddDigit(grid, extraCell) ?: return null

        return Deduction(
            technique = id,
            focusCells = listOf(extraCell),
            focusCandidates = grid.candidatesAt(extraCell).toList().map { CellDigit(extraCell, it) },
            houses = grid.housesOf(extraCell),
            placements = listOf(CellDigit(extraCell, odd)),
        )
    }

    /**
     * The one candidate of [extraCell] that appears three times in each of its houses,
     * or null when the grid is not actually one candidate away from a grave.
     */
    private fun oddDigit(grid: CandidateGrid, extraCell: Int): Int? {
        val ownHouses = grid.housesOf(extraCell).toSet()
        var odd = -1

        for (house in grid.houses) {
            val cells = grid.cellsOf(house)
            for (digit in 1..grid.size) {
                if (grid.isPlacedIn(house, digit)) continue
                val count = cells.count { digit in grid.candidatesAt(it) }
                if (count == 0) return null
                if (count == 2) continue
                // Only the odd cell's houses may deviate, only by one digit, and it must be
                // the same digit every time.
                if (count != 3 || house !in ownHouses) return null
                if (digit !in grid.candidatesAt(extraCell)) return null
                if (odd == -1) odd = digit else if (odd != digit) return null
            }
        }
        return if (odd == -1) null else odd
    }
}
