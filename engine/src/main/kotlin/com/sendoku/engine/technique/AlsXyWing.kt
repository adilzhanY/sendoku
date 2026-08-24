package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * ALS-XY-Wing. Three almost locked sets in a row, the middle one holding the other two apart.
 *
 * Take three groups, each one digit away from using up everything it holds. Call them A, the
 * hinge, and C. A shares a digit x with the hinge, and x is restricted, meaning every place
 * it could go in one group sees every place it could go in the other, so only one of the two
 * can actually take it. The hinge shares a second, different digit y with C, restricted the
 * same way.
 *
 * Now suppose some cell outside all three takes a digit z that both A and C could hold, and
 * suppose it sees every place z could go in either of them. Then A does not take z, so A is
 * locked and must use everything else it holds, x included. The hinge therefore cannot have
 * x, so the hinge is locked too, and must use y. Then C cannot have y, so C is locked in its
 * turn and must use everything it holds, which includes z. But z was supposed to be gone
 * from C. The assumption collapses, and the cell loses z.
 *
 * Three groups and two restricted digits is one more link than an ALS-XZ, and the chain runs
 * the same way an XY-Wing does, which is where the name comes from. It is the cheapest rule
 * on the ladder that no mainstream app implements at all.
 */
public object AlsXyWing : Technique {

    /**
     * Cells per set. One below what ALS-XZ allows.
     *
     * Three nested loops over the sets is the cost here, and four cell sets roughly triple
     * how many there are. Measured on the rating corpus, capping at three keeps this rule
     * inside the speed budget while losing very few real patterns, because a wing built from
     * three four cell sets is beyond what a person would ever spot anyway.
     */
    private const val MAX_CELLS = 3

    override val id: TechniqueId get() = TechniqueId.ALS_XY_WING

    override fun find(grid: CandidateGrid): Deduction? {
        val sets = AlsFinder.collect(grid, MAX_CELLS)
        if (sets.size < 3) return null

        val links = restrictedLinks(grid, sets)
        if (links.isEmpty()) return null

        for ((hinge, fromHinge) in links) {
            for ((leftIndex, x) in fromHinge) {
                for ((rightIndex, y) in fromHinge) {
                    if (x == y || leftIndex == rightIndex) continue
                    val left = sets[leftIndex]
                    val right = sets[rightIndex]
                    if (left.overlaps(right)) continue

                    val found = wing(grid, left, sets[hinge], right, x, y)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun wing(grid: CandidateGrid, left: Als, hinge: Als, right: Als, x: Int, y: Int): Deduction? {
        val shared = left.candidates and right.candidates
        for (z in shared.toList()) {
            if (z == x || z == y) continue

            val leftHomes = AlsFinder.homes(grid, left, z)
            val rightHomes = AlsFinder.homes(grid, right, z)
            if (leftHomes.isEmpty() || rightHomes.isEmpty()) continue

            val inside = left.cells + hinge.cells + right.cells
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
                focusCandidates = AlsFinder.marksOf(grid, inside),
                houses = listOf(left.house, hinge.house, right.house).distinct(),
                eliminations = targets.map { CellDigit(it, z) },
            )
        }
        return null
    }

    /**
     * For each set, the sets it shares a restricted digit with.
     *
     * Built once. The same pair would otherwise be tested from both ends and from every
     * hinge that touches it, which is the difference between this rule running in a
     * millisecond and running in fifty.
     */
    private fun restrictedLinks(grid: CandidateGrid, sets: List<Als>): Map<Int, List<Pair<Int, Int>>> {
        val links = HashMap<Int, MutableList<Pair<Int, Int>>>()
        for (first in sets.indices) {
            for (second in first + 1 until sets.size) {
                if (sets[first].overlaps(sets[second])) continue
                val shared = sets[first].candidates and sets[second].candidates
                if (shared.isEmpty) continue
                shared.forEach { digit ->
                    if (AlsFinder.restricted(grid, sets[first], sets[second], digit)) {
                        links.getOrPut(first) { ArrayList() }.add(second to digit)
                        links.getOrPut(second) { ArrayList() }.add(first to digit)
                    }
                }
            }
        }
        return links
    }
}
