package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * A cell with one candidate left must hold it.
 *
 * The cheapest rule there is, and the one that does most of the work on an easy grid.
 * It looks only at the cell, never at the rest of a row or box, which is what
 * separates it from a hidden single.
 */
public object NakedSingle : Technique {

    override val id: TechniqueId get() = TechniqueId.NAKED_SINGLE

    override fun find(grid: CandidateGrid): Deduction? {
        for (index in 0 until grid.cellCount) {
            if (!grid.isEmpty(index)) continue
            val candidates = grid.candidatesAt(index)
            if (!candidates.isSingle) continue
            val digit = candidates.single
            return Deduction(
                technique = id,
                focusCells = listOf(index),
                focusCandidates = listOf(CellDigit(index, digit)),
                placements = listOf(CellDigit(index, digit)),
            )
        }
        return null
    }
}
