package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates
import com.sendoku.engine.House

/**
 * XY-Wing. Three cells of two candidates each, arranged so that one of two digits must
 * land somewhere the third digit cannot survive.
 *
 * A pivot holds `{x, y}`. One pincer sees the pivot and holds `{x, z}`, the other sees
 * the pivot and holds `{y, z}`. The pivot is x or y. If it is x the second pincer is z,
 * if it is y the first pincer is z. Either way some pincer is z, so nothing that sees
 * both pincers can be z.
 */
public object XYWing : Technique {

    override val id: TechniqueId get() = TechniqueId.XY_WING

    override fun find(grid: CandidateGrid): Deduction? {
        for (pivot in 0 until grid.cellCount) {
            val pair = grid.candidatesAt(pivot)
            if (pair.size != 2) continue
            val digits = pair.toList()

            for (first in digits) {
                val second = digits.first { it != first }
                for (left in grid.peersOf(pivot)) {
                    val leftPair = grid.candidatesAt(left)
                    if (leftPair.size != 2 || first !in leftPair || second in leftPair) continue
                    val z = leftPair.toList().first { it != first }

                    for (right in grid.peersOf(pivot)) {
                        if (right == left) continue
                        val rightPair = grid.candidatesAt(right)
                        if (rightPair != Candidates.of(second, z)) continue

                        val targets = grid.seenByBoth(left, right)
                            .filter { it != pivot && z in grid.candidatesAt(it) }
                        if (targets.isEmpty()) continue

                        return Deduction(
                            technique = id,
                            focusCells = listOf(pivot, left, right).sorted(),
                            focusCandidates = listOf(
                                CellDigit(pivot, first),
                                CellDigit(pivot, second),
                                CellDigit(left, z),
                                CellDigit(right, z),
                            ),
                            eliminations = targets.map { CellDigit(it, z) },
                        )
                    }
                }
            }
        }
        return null
    }
}

/**
 * XYZ-Wing. The pivot joins in.
 *
 * The pivot holds all three digits `{x, y, z}` rather than two, so it can be z itself.
 * That weakens the conclusion: a cell must now see the pivot as well as both pincers
 * before z can be struck from it.
 */
public object XYZWing : Technique {

    override val id: TechniqueId get() = TechniqueId.XYZ_WING

    override fun find(grid: CandidateGrid): Deduction? {
        for (pivot in 0 until grid.cellCount) {
            val triple = grid.candidatesAt(pivot)
            if (triple.size != 3) continue
            val peers = grid.peersOf(pivot)

            for (leftIndex in peers.indices) {
                val left = peers[leftIndex]
                val leftPair = grid.candidatesAt(left)
                if (leftPair.size != 2 || !triple.containsAll(leftPair)) continue

                for (rightIndex in leftIndex + 1 until peers.size) {
                    val right = peers[rightIndex]
                    val rightPair = grid.candidatesAt(right)
                    if (rightPair.size != 2 || !triple.containsAll(rightPair)) continue
                    if ((leftPair or rightPair) != triple) continue

                    val shared = leftPair and rightPair
                    if (!shared.isSingle) continue
                    val z = shared.single

                    val targets = grid.seenByBoth(left, right)
                        .filter { it != pivot && grid.sees(it, pivot) && z in grid.candidatesAt(it) }
                    if (targets.isEmpty()) continue

                    return Deduction(
                        technique = id,
                        focusCells = listOf(pivot, left, right).sorted(),
                        focusCandidates = listOf(CellDigit(pivot, z), CellDigit(left, z), CellDigit(right, z)),
                        eliminations = targets.map { CellDigit(it, z) },
                    )
                }
            }
        }
        return null
    }
}

/**
 * W-Wing. Two cells holding the same pair, joined by a strong link on one of the digits.
 *
 * Suppose neither of the two cells is y. Then both are x, which kills x in both ends of
 * the strong link, leaving that house with nowhere to put x. So at least one of the two
 * cells is y, and nothing that sees both of them can be y.
 *
 * Unlike the other wings this one needs no shared region at all. The two cells can sit
 * anywhere, which is exactly why it is hard to spot.
 */
public object WWing : Technique {

    override val id: TechniqueId get() = TechniqueId.W_WING

    override fun find(grid: CandidateGrid): Deduction? {
        val bivalue = (0 until grid.cellCount).filter { grid.candidatesAt(it).size == 2 }

        for (firstIndex in bivalue.indices) {
            val a = bivalue[firstIndex]
            val pair = grid.candidatesAt(a)

            for (secondIndex in firstIndex + 1 until bivalue.size) {
                val b = bivalue[secondIndex]
                if (grid.candidatesAt(b) != pair) continue
                if (grid.sees(a, b)) continue

                val targets = grid.seenByBoth(a, b)
                if (targets.isEmpty()) continue

                for (x in pair.toList()) {
                    val y = pair.toList().first { it != x }
                    val link = strongLink(grid, x, a, b) ?: continue
                    val struck = targets.filter { y in grid.candidatesAt(it) }
                    if (struck.isEmpty()) continue

                    return Deduction(
                        technique = id,
                        focusCells = listOf(a, b),
                        focusCandidates = listOf(CellDigit(a, y), CellDigit(b, y)) +
                            link.second.map { CellDigit(it, x) },
                        houses = listOf(link.first),
                        eliminations = struck.map { CellDigit(it, y) },
                    )
                }
            }
        }
        return null
    }

    /**
     * A house where [digit] has exactly two homes, one seeing [a] and the other seeing [b].
     * Neither home may be [a] or [b] themselves, or the argument collapses.
     */
    private fun strongLink(grid: CandidateGrid, digit: Int, a: Int, b: Int): Pair<House, List<Int>>? {
        for (house in grid.houses) {
            if (grid.isPlacedIn(house, digit)) continue
            val homes = grid.cellsOf(house).filter { digit in grid.candidatesAt(it) }
            if (homes.size != 2) continue
            val (first, second) = homes
            if (first == a || first == b || second == a || second == b) continue
            val joins = (grid.sees(first, a) && grid.sees(second, b)) ||
                (grid.sees(first, b) && grid.sees(second, a))
            if (joins) return house to homes
        }
        return null
    }
}

/** The cells that see both [a] and [b], excluding the two themselves. */
internal fun CandidateGrid.seenByBoth(a: Int, b: Int): List<Int> =
    peersOf(a).filter { it != b && sees(it, b) }
