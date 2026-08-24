package com.sendoku.engine.technique

import com.sendoku.engine.CandidateGrid
import com.sendoku.engine.House

/**
 * Every human technique the solver knows, with what it costs a player to see.
 *
 * The cost is the whole point. It is what turns a solve path into a difficulty rating, so
 * the numbers are not decoration: they decide which grade a puzzle ships under. They are
 * calibrated against the published difficulty tables that solvers like Sudoku Explainer
 * use, on the same rough one to nine scale, so a Sendoku rating means something to anyone
 * who already knows those.
 *
 * Declaration order below is grouping, not difficulty. The ladder the solver actually
 * walks is sorted by [cost], see `Techniques.ladder`.
 */
public enum class TechniqueId(public val displayName: String, public val cost: Double) {
    NAKED_SINGLE("Naked single", 1.0),
    HIDDEN_SINGLE("Hidden single", 1.5),
    LOCKED_CANDIDATES_POINTING("Pointing pair or triple", 2.6),
    LOCKED_CANDIDATES_CLAIMING("Claiming pair or triple", 2.8),
    NAKED_PAIR("Naked pair", 3.0),
    HIDDEN_PAIR("Hidden pair", 3.4),
    NAKED_TRIPLE("Naked triple", 3.6),
    HIDDEN_TRIPLE("Hidden triple", 4.0),
    NAKED_QUAD("Naked quad", 5.0),
    HIDDEN_QUAD("Hidden quad", 5.4),
    X_WING("X-Wing", 3.2),
    SIMPLE_COLOURING("Simple colouring", 4.8),
    XY_WING("XY-Wing", 4.2),
    XYZ_WING("XYZ-Wing", 4.4),
    W_WING("W-Wing", 4.4),
    SWORDFISH("Swordfish", 3.8),
    REMOTE_PAIRS("Remote pairs", 5.0),
    UNIQUE_RECTANGLE("Unique rectangle", 4.6),
    BUG_PLUS_ONE("BUG plus one", 5.6),
    JELLYFISH("Jellyfish", 5.2),
    MULTI_COLOURING("Multi colouring", 5.6),
    X_CHAIN("X-Chain", 6.6),
    XY_CHAIN("XY-Chain", 6.8),
    SUE_DE_COQ("Sue de Coq", 7.4),
    ALS_XZ("ALS XZ", 7.5),
    ALS_XY_WING("ALS XY-Wing", 8.4),
    DEATH_BLOSSOM("Death blossom", 8.8),
    FORCING_CHAIN("Forcing chain", 9.0),
}

/** One digit in one cell. The cell is a row-major index into the grid. */
public data class CellDigit(val cell: Int, val digit: Int)

/**
 * One step of human logic, and everything the app needs to draw it.
 *
 * A deduction is inert. Finding it changes nothing, which is what lets the hint system
 * show a step before the player commits to it. [apply] is the only thing that writes.
 *
 * @param technique which rule fired
 * @param focusCells the cells the logic rests on, highlighted by a hint
 * @param focusCandidates the specific pencil marks the logic rests on
 * @param houses the regions the logic rests on, so a hint can outline them
 * @param placements digits proved to belong in a cell
 * @param eliminations pencil marks proved impossible
 */
public data class Deduction(
    val technique: TechniqueId,
    val focusCells: List<Int> = emptyList(),
    val focusCandidates: List<CellDigit> = emptyList(),
    val houses: List<House> = emptyList(),
    val placements: List<CellDigit> = emptyList(),
    val eliminations: List<CellDigit> = emptyList(),
) {

    init {
        require(placements.isNotEmpty() || eliminations.isNotEmpty()) {
            "$technique produced a deduction that changes nothing"
        }
    }

    /** Cells this step writes to, whether by placing a digit or striking a mark. */
    public val changedCells: List<Int>
        get() = (placements.map { it.cell } + eliminations.map { it.cell }).distinct()
}

/**
 * A rule that reads a grid and proposes one step.
 *
 * Implementations are stateless and must not modify the grid they are handed. Returning
 * null means "this rule has nothing to say about this grid right now", which is the
 * normal case for all but one or two techniques on any given board.
 */
public interface Technique {

    public val id: TechniqueId

    /** The first step this rule can prove, or null when it can prove none. */
    public fun find(grid: CandidateGrid): Deduction?
}

/**
 * Writes a deduction into a grid. Returns true when anything actually changed.
 *
 * Eliminations go first so that a step which both strikes marks and places a digit
 * leaves the grid consistent either way.
 */
public fun CandidateGrid.apply(deduction: Deduction): Boolean {
    var changed = false
    for ((cell, digit) in deduction.eliminations) {
        if (eliminate(cell, digit)) changed = true
    }
    for ((cell, digit) in deduction.placements) {
        place(cell, digit)
        changed = true
    }
    return changed
}
