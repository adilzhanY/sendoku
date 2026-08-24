package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid

/**
 * Death Blossom. One cell, and a group hanging off each digit it could be.
 *
 * Pick a cell with two or three candidates. Call it the stem. For each digit the stem could
 * take, find a group of cells that is one digit short of being full and that holds that
 * digit, arranged so that every place the digit could go inside the group is seen by the
 * stem. Those groups are the petals, one per digit, and there is no choice about which
 * petal answers which digit.
 *
 * Now whatever the stem turns out to be, its digit is knocked out of the matching petal,
 * because the stem sees every place that digit could have gone. That petal is then full,
 * and a full group uses every digit it holds.
 *
 * So take a digit z that every petal could hold. Whichever way the stem falls, some petal
 * is full and has to place z. Any cell outside the flower that can see z in every petal
 * therefore cannot be z itself.
 *
 * The name is somebody else's and it is a good one: a stem with petals, all of which die
 * together.
 */
public object DeathBlossom : Technique {

    /**
     * Digits the stem may hold.
     *
     * Two or three. A four candidate stem needs four petals and the search grows with the
     * product, which buys a pattern nobody would find at a cost the phone would notice.
     */
    private const val MAX_STEM = 3

    /** Cells per petal, kept small for the same reason [AlsXyWing] keeps its sets small. */
    private const val MAX_PETAL = 3

    /**
     * Petals kept per stem digit.
     *
     * The search is a product across the stem's digits, so this is the number that decides
     * whether the rule costs a millisecond or a second. Twelve is well past the point where
     * extra petals find anything: they are tried in the order the sets were collected,
     * which is smallest first, and a real flower is built from small petals.
     */
    private const val MAX_PETALS_PER_DIGIT = 12

    override val id: TechniqueId get() = TechniqueId.DEATH_BLOSSOM

    override fun find(grid: CandidateGrid): Deduction? {
        val sets = AlsFinder.collect(grid, MAX_PETAL)
        if (sets.isEmpty()) return null

        for (stem in 0 until grid.cellCount) {
            if (!grid.isEmpty(stem)) continue
            val digits = grid.candidatesAt(stem)
            if (digits.size < 2 || digits.size > MAX_STEM) continue

            val choices = digits.toList().map { digit -> petalsFor(grid, sets, stem, digit) }
            if (choices.any { it.isEmpty() }) continue

            val found = combine(grid, stem, digits.toList(), choices, ArrayList())
            if (found != null) return found
        }
        return null
    }

    /**
     * Groups that could answer for [digit] if the stem takes it.
     *
     * The stem has to see every home of the digit inside the group, or the group would
     * survive the stem taking it and prove nothing. The stem itself must stay outside.
     */
    private fun petalsFor(grid: CandidateGrid, sets: List<Als>, stem: Int, digit: Int): List<Als> {
        val petals = ArrayList<Als>()
        for (set in sets) {
            if (stem in set.cells) continue
            if (digit !in set.candidates) continue
            val homes = AlsFinder.homes(grid, set, digit)
            if (homes.isEmpty() || !homes.all { grid.sees(stem, it) }) continue
            petals.add(set)
            if (petals.size == MAX_PETALS_PER_DIGIT) break
        }
        return petals
    }

    /** One petal per stem digit, then the elimination the whole flower supports. */
    private fun combine(
        grid: CandidateGrid,
        stem: Int,
        digits: List<Int>,
        choices: List<List<Als>>,
        picked: MutableList<Als>,
    ): Deduction? {
        if (picked.size == digits.size) return bloom(grid, stem, digits, picked)

        for (petal in choices[picked.size]) {
            if (picked.any { it.overlaps(petal) }) continue
            picked.add(petal)
            val found = combine(grid, stem, digits, choices, picked)
            picked.removeAt(picked.lastIndex)
            if (found != null) return found
        }
        return null
    }

    private fun bloom(grid: CandidateGrid, stem: Int, digits: List<Int>, petals: List<Als>): Deduction? {
        var shared = petals.first().candidates
        for (petal in petals) shared = shared and petal.candidates
        // A digit the stem itself holds is the one the flower is arguing about, not one it
        // can conclude anything with.
        for (digit in digits) shared -= digit
        if (shared.isEmpty) return null

        val inside = petals.flatMap { it.cells } + stem
        for (z in shared.toList()) {
            val homes = petals.map { AlsFinder.homes(grid, it, z) }
            if (homes.any { it.isEmpty() }) continue

            val targets = (0 until grid.cellCount).filter { cell ->
                cell !in inside &&
                    z in grid.candidatesAt(cell) &&
                    homes.all { petal -> petal.all { grid.sees(cell, it) } }
            }
            if (targets.isEmpty()) continue

            return Deduction(
                technique = id,
                focusCells = inside.sorted(),
                focusCandidates = AlsFinder.marksOf(grid, inside),
                houses = petals.map { it.house }.distinct(),
                eliminations = targets.map { CellDigit(it, z) },
            )
        }
        return null
    }
}
