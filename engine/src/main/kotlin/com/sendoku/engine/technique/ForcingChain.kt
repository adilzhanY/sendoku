package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * Forcing chain. Take a cell with two possibilities and follow both of them.
 *
 * Every other rule on the ladder finds a pattern. This one does not: it picks a cell that
 * could be one of two digits, assumes the first, and follows the easy consequences as far
 * as they go. Then it puts the grid back and does the same with the second.
 *
 * Two things can come out of that. Either one of the two assumptions runs into a cell with
 * nothing left to put in it, which means that digit was never possible and can be struck
 * off. Or both assumptions end up writing the same digit into the same cell somewhere else,
 * which means that digit belongs there whichever way the fork falls.
 *
 * This is not guessing, and the difference is worth being precise about. A guess is putting
 * a digit in and carrying on in the hope it works out. This follows both branches to the
 * end and only keeps what is true in every one of them, so nothing here rests on the
 * assumption being right. It is the same shape of argument as proof by cases, and it is
 * what people mean when they say a puzzle has to be worked out on paper.
 *
 * The consequences followed are the two cheapest rules only: a cell with one candidate
 * left, and a digit with one home left in a house. Anything deeper and the technique would
 * be the whole solver running inside itself, which is both slow and impossible to explain
 * in a hint.
 */
public object ForcingChain : Technique {

    /**
     * Cells the fork is tried from.
     *
     * Every bivalue cell in the grid would be the thorough choice. It is also the slowest
     * rule on the ladder by a distance, and it only ever runs when everything cheaper has
     * failed, which is exactly the position where the solver is already working hard. The
     * cap keeps the worst case inside the phone budget, and a puzzle that needs the
     * fortieth fork rather than one of the first is vanishingly rare.
     */
    private const val MAX_FORKS = 40

    /** Steps followed down one branch before giving up on it. */
    private const val MAX_DEPTH = 120

    override val id: TechniqueId get() = TechniqueId.FORCING_CHAIN

    override fun find(grid: CandidateGrid): Deduction? {
        var forks = 0
        for (cell in 0 until grid.cellCount) {
            if (!grid.isEmpty(cell)) continue
            val digits = grid.candidatesAt(cell)
            if (digits.size != 2) continue

            val branches = digits.toList().map { digit -> follow(grid, cell, digit) }
            val found = conclude(cell, digits.toList(), branches)
            if (found != null) return found

            forks++
            if (forks >= MAX_FORKS) return null
        }
        return null
    }

    /** What one assumption leads to: the digits it writes, or nothing if it breaks the grid. */
    private fun follow(grid: CandidateGrid, cell: Int, digit: Int): Branch {
        val copy = grid.copy()
        copy.place(cell, digit)
        if (copy.hasContradiction) return Branch(broken = true, placed = emptyMap())

        val placed = HashMap<Int, Int>()
        placed[cell] = digit
        repeat(MAX_DEPTH) {
            if (copy.isSolved) return Branch(broken = false, placed = placed)
            val step = NakedSingle.find(copy) ?: HiddenSingle.find(copy) ?: return Branch(false, placed)
            copy.apply(step)
            for ((at, value) in step.placements) placed[at] = value
            if (copy.hasContradiction) return Branch(broken = true, placed = emptyMap())
        }
        return Branch(broken = false, placed = placed)
    }

    private fun conclude(cell: Int, digits: List<Int>, branches: List<Branch>): Deduction? {
        // A branch that breaks the grid is the stronger answer, and the easier one to say:
        // that digit was never possible.
        for ((index, branch) in branches.withIndex()) {
            if (branch.broken) {
                return Deduction(
                    technique = id,
                    focusCells = listOf(cell),
                    focusCandidates = digits.map { CellDigit(cell, it) },
                    eliminations = listOf(CellDigit(cell, digits[index])),
                )
            }
        }

        // Otherwise, anything both branches agree on is true whichever way the fork falls.
        val first = branches[0].placed
        val second = branches[1].placed
        for ((at, value) in first) {
            if (at == cell) continue
            if (second[at] != value) continue
            return Deduction(
                technique = id,
                focusCells = listOf(cell, at),
                focusCandidates = digits.map { CellDigit(cell, it) } + CellDigit(at, value),
                placements = listOf(CellDigit(at, value)),
            )
        }
        return null
    }

    /** One assumption followed to its end. [broken] means the grid could not survive it. */
    private class Branch(val broken: Boolean, val placed: Map<Int, Int>)
}
