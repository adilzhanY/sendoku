package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.Candidates

/**
 * Remote pairs. A chain of cells that all hold the same two candidates.
 *
 * Neighbours in the chain see each other, so they take opposite digits. Follow the chain
 * and the values alternate. Two cells an odd number of steps apart therefore hold one
 * digit each, in some order, and anything that sees both of them can hold neither.
 *
 * Two steps apart proves nothing, and one step apart is just a naked pair. The rule only
 * earns its place from three steps on, which is why it looks for cells that alternate but
 * do not see each other.
 */
public object RemotePairs : Technique {

    override val id: TechniqueId get() = TechniqueId.REMOTE_PAIRS

    override fun find(grid: CandidateGrid): Deduction? {
        val byPair = HashMap<Candidates, MutableList<Int>>()
        for (cell in 0 until grid.cellCount) {
            val pair = grid.candidatesAt(cell)
            if (pair.size != 2) continue
            byPair.getOrPut(pair) { ArrayList() }.add(cell)
        }

        for ((pair, cells) in byPair.entries.sortedBy { it.key.mask }) {
            if (cells.size < 4) continue
            val deduction = search(grid, pair, cells)
            if (deduction != null) return deduction
        }
        return null
    }

    private fun search(grid: CandidateGrid, pair: Candidates, cells: List<Int>): Deduction? {
        val step = HashMap<Int, Int>()
        val seen = HashSet<Int>()

        for (seed in cells) {
            if (seed in seen) continue
            val group = ArrayList<Int>()
            step[seed] = 0
            seen.add(seed)
            val queue = ArrayDeque<Int>()
            queue.add(seed)
            while (queue.isNotEmpty()) {
                val cell = queue.removeFirst()
                group.add(cell)
                for (next in cells) {
                    if (next in seen || !grid.sees(cell, next)) continue
                    seen.add(next)
                    step[next] = step.getValue(cell) + 1
                    queue.add(next)
                }
            }
            if (group.size < 4) continue

            val sorted = group.sorted()
            for (leftIndex in sorted.indices) {
                for (rightIndex in leftIndex + 1 until sorted.size) {
                    val left = sorted[leftIndex]
                    val right = sorted[rightIndex]
                    // Different parity means an odd number of steps, so one holds each digit.
                    // Seeing each other would make it a plain naked pair.
                    if ((step.getValue(left) - step.getValue(right)) % 2 == 0) continue
                    if (grid.sees(left, right)) continue

                    val targets = grid.seenByBoth(left, right).filter { it !in group }
                    val eliminations = buildList {
                        for (cell in targets) {
                            (grid.candidatesAt(cell) and pair).forEach { add(CellDigit(cell, it)) }
                        }
                    }.sortedWith(compareBy({ it.cell }, { it.digit }))
                    if (eliminations.isEmpty()) continue

                    return Deduction(
                        technique = id,
                        focusCells = sorted,
                        focusCandidates = sorted.flatMap { cell ->
                            pair.toList().map { CellDigit(cell, it) }
                        },
                        eliminations = eliminations,
                    )
                }
            }
        }
        return null
    }
}
