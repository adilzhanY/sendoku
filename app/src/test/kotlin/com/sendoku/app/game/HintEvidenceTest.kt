package com.sendoku.app.game

import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The working behind "only one digit is left here".
 *
 * Somebody sent a screenshot of a hint saying exactly that, next to a grid where the digit
 * in question could still go in thirty other cells. Both statements were true: the cell had
 * one candidate, and the digit had many homes. They are different questions, and the app was
 * answering one while the player was checking the other, with nothing on screen to tell them
 * apart. The hint was right and unusable, which is the same as wrong.
 *
 * So a naked single now carries its arithmetic, and these tests are about that arithmetic
 * being true rather than merely present.
 */
class HintEvidenceTest {

    private val puzzle = CatalogReader
        .from(checkNotNull(javaClass.getResourceAsStream("/catalog/classic.sdkb")))
        .let { catalog -> catalog.puzzleAt(catalog.indicesOf(Grade.GENTLE).first()) }

    private fun firstSingle(): Pair<GameState, Hint.Step> {
        var state = GameState.start(puzzle)
        repeat(200) {
            val hint = HintEngine.next(state, HintLevel.FULL) as? Hint.Step ?: return@repeat
            if (hint.deduction.technique == TechniqueId.NAKED_SINGLE) return state to hint
            state = state.applyHint(hint.deduction)
        }
        error("no naked single turned up, so nothing was tested")
    }

    @Test
    fun `a single carries what its row, column and box already hold`() {
        val (state, hint) = firstSingle()
        val evidence = requireNotNull(hint.evidence) { "a single arrived with no working" }
        val cell = hint.deduction.placements.first().cell

        val geometry = com.sendoku.engine.Geometry.of(state.dims)
        fun digitsIn(house: com.sendoku.engine.House) = geometry.cellsOf(house)
            .map { state.cells[it].digit }
            .filter { it != 0 }
            .distinct()
            .sorted()

        assertEquals(digitsIn(com.sendoku.engine.House(com.sendoku.engine.HouseKind.ROW, cell / 9)), evidence.row)
        assertEquals(
            digitsIn(com.sendoku.engine.House(com.sendoku.engine.HouseKind.COLUMN, cell % 9)),
            evidence.column,
        )
        assertEquals(
            digitsIn(com.sendoku.engine.House(com.sendoku.engine.HouseKind.BOX, state.dims.boxOf(cell / 9, cell % 9))),
            evidence.box,
        )
    }

    @Test
    fun `the three lists really do account for every digit but one`() {
        // The claim the player is being asked to check. If this ever fails, the app is
        // showing somebody arithmetic that does not add up, which is worse than saying
        // nothing at all.
        val (_, hint) = firstSingle()
        val evidence = requireNotNull(hint.evidence) { "a single arrived with no working" }
        val taken = (evidence.row + evidence.column + evidence.box).toSet()

        assertEquals("the working leaves more than one digit", setOf(evidence.digit), (1..9).toSet() - taken)
        assertTrue("the digit it names is already somewhere in its own houses", evidence.digit !in taken)
    }

    @Test
    fun `the cell keeps the three houses that ruled the others out`() {
        val (_, hint) = firstSingle()
        assertEquals("a single should name its row, its column and its box", 3, hint.deduction.houses.size)
    }

    @Test
    fun `a rule that is not about one cell carries no such working`() {
        // The working is specific to a naked single. Bolting it onto a chain would be a
        // sentence that sounds like an explanation and is not one.
        var state = GameState.start(puzzle)
        repeat(200) {
            val hint = HintEngine.next(state, HintLevel.FULL) as? Hint.Step ?: return@repeat
            if (hint.deduction.technique != TechniqueId.NAKED_SINGLE) {
                assertNull("${hint.deduction.technique} was given a single's working", hint.evidence)
                return
            }
            state = state.applyHint(hint.deduction)
        }
    }
}
