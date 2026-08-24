package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates

/**
 * ALS-XZ. Two almost locked sets, joined by a digit that can only live in one of them.
 *
 * Call that digit x. Because x is restricted, at most one set actually takes it, so the
 * other set loses x, becomes locked, and is forced to use every digit it has left. Both
 * cases are covered, so whichever way it falls, one of the two sets is locked.
 *
 * Now take any other digit z the two sets share. The locked set must use z somewhere, so
 * z lands inside one set or the other. A cell outside both that sees every home of z in
 * both sets therefore cannot be z.
 *
 * This is the first rule on the ladder that reasons about a group of cells acting as one,
 * which is what puts it beyond what most apps call extreme. Everything above it on the
 * ladder is built from the same idea.
 */
public object AlsXz : Technique {

    override val id: TechniqueId get() = TechniqueId.ALS_XZ

    override fun find(grid: CandidateGrid): Deduction? {
        val sets = AlsFinder.collect(grid)
        if (sets.size < 2) return null

        for (leftIndex in sets.indices) {
            val left = sets[leftIndex]
            for (rightIndex in leftIndex + 1 until sets.size) {
                val right = sets[rightIndex]
                if (left.cells.any { it in right.cells }) continue

                val shared = left.candidates and right.candidates
                if (shared.size < 2) continue

                val deduction = pair(grid, left, right, shared)
                if (deduction != null) return deduction
            }
        }
        return null
    }

    private fun pair(grid: CandidateGrid, left: Als, right: Als, shared: Candidates): Deduction? {
        for (x in shared.toList()) {
            if (!AlsFinder.restricted(grid, left, right, x)) continue

            for (z in shared.toList()) {
                if (z == x) continue
                val leftHomes = left.cells.filter { z in grid.candidatesAt(it) }
                val rightHomes = right.cells.filter { z in grid.candidatesAt(it) }
                if (leftHomes.isEmpty() || rightHomes.isEmpty()) continue

                val inside = left.cells + right.cells
                val targets = (0 until grid.cellCount).filter { cell ->
                    cell !in inside &&
                        z in grid.candidatesAt(cell) &&
                        leftHomes.all { grid.sees(cell, it) } &&
                        rightHomes.all { grid.sees(cell, it) }
                }
                if (targets.isEmpty()) continue

                return Deduction(
                    technique = id,
                    focusCells = inside.sorted(),
                    focusCandidates = inside.sorted().flatMap { cell ->
                        grid.candidatesAt(cell).toList().map { CellDigit(cell, it) }
                    },
                    houses = listOf(left.house, right.house),
                    eliminations = targets.map { CellDigit(it, z) },
                )
            }
        }
        return null
    }
}
