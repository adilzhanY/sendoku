package com.sendoku.app.game

import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.SavedGame
import com.sendoku.app.data.Statistics
import com.sendoku.engine.Board
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.killer.KillerCatalogReader
import com.sendoku.engine.killer.KillerGenerator
import com.sendoku.engine.killer.KillerRater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration

/**
 * A Killer as the app plays it.
 *
 * The cages are the only new thing in the game state, and everything hangs off them: the
 * board draws them, the conflict marking uses them, and a game put down halfway has to come
 * back with them. An ordinary puzzle carries an empty list and behaves exactly as before,
 * which is the property worth testing hardest, because a variant that changes the classic
 * game is a variant that broke it.
 */
class KillerGameTest {

    private val classic = Dimensions.CLASSIC

    /** An ordinary puzzle, so the cage rules can be checked to stay out of its way. */
    private val realGivens =
        "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"

    private fun killer(seed: Int = 4): GameState {
        val puzzle = checkNotNull(KillerGenerator(classic, Random(seed), sizes = 2..4).next())
        val report = KillerRater(puzzle).solve()
        return GameState.start(
            rated = RatedPuzzle(
                puzzle = Puzzle(givens = Board(classic), solution = puzzle.solution),
                rating = report.rating,
                grade = report.grade,
                hardest = report.hardest,
                symmetry = Symmetry.NONE,
                usage = report.usage,
            ),
            origin = PuzzleOrigin.KILLER,
            cages = puzzle.cages,
        )
    }

    @Test
    fun aKillerStartsEmptyAndCoveredByCages() {
        val state = killer()
        assertTrue("a Killer was dealt with clues in it", state.cells.none { it.isGiven })
        assertEquals(classic.cellCount, state.cages.sumOf { it.size })
    }

    @Test
    fun aRepeatedDigitInACageIsMarked() {
        // A cage never repeats a digit. Nothing else in the game knows that rule, so the
        // board has to.
        var state = killer()
        val cage = state.cages.first { it.size >= 2 }
        val digit = state.solution.atIndex(cage.cells[0])
        state = state.select(cage.cells[0]).enter(digit)
        state = state.select(cage.cells[1]).enter(digit)
        assertTrue("a repeat inside a cage was not marked", cage.cells[1] in state.conflicts)
    }

    @Test
    fun aCageThatHasOverrunItsSumIsMarked() {
        var state = killer()
        val cage = state.cages.first { it.size == 2 && it.sum < 12 }
        // Two digits that add to more than the cage allows, and do not repeat.
        state = state.select(cage.cells[0]).enter(9)
        state = state.select(cage.cells[1]).enter(8)
        assertTrue("a cage over its sum was not marked", cage.cells[0] in state.conflicts)
    }

    @Test
    fun anOrdinaryPuzzleHasNoCagesAndNoCageMarking() {
        val plain = GameState.start(
            RatedPuzzle(
                puzzle = Puzzle(givens = Board(classic), solution = Board(classic)),
                rating = 1.0,
                grade = com.sendoku.engine.Grade.GENTLE,
                hardest = null,
                symmetry = Symmetry.NONE,
                usage = emptyMap(),
            ),
        )
        assertTrue(plain.cages.isEmpty())
        assertTrue(plain.conflicts.isEmpty())
    }

    @Test
    fun theCagesSurviveBeingWrittenDown() {
        val state = killer()
        val back = SavedGame.of(state).toState(GameSettings())
        assertEquals(state.cages, back.cages)
        assertEquals(state.cages.map { it.sum }, back.cages.map { it.sum })
    }

    @Test
    fun anOrdinaryPuzzleWritesNoCages() {
        val plain = GameState.start(
            RatedPuzzle(
                puzzle = Puzzle(givens = Board(classic), solution = Board(classic)),
                rating = 1.0,
                grade = com.sendoku.engine.Grade.GENTLE,
                hardest = null,
                symmetry = Symmetry.NONE,
                usage = emptyMap(),
            ),
        )
        assertEquals("", SavedGame.of(plain).cages)
    }

    @Test
    fun winningAKillerOpensNoOrdinaryLevel() {
        // A Killer is a different game on the same grid. Winning one says nothing about
        // whether somebody is ready for a harder ordinary puzzle.
        assertFalse(PuzzleOrigin.KILLER.earnsProgress)
    }

    @Test
    fun aHintOnAKillerCanBeAboutACage() {
        // The hint engine walks both ladders mixed by cost on a Killer, so on an empty
        // Killer board the first thing it has to say is a cage rule: nothing else can fire
        // on a grid with no digits in it at all.
        val state = killer()
        val hint = HintEngine.next(state)
        assertTrue("an empty Killer produced $hint", hint is Hint.Step)
        val step = hint as Hint.Step
        assertTrue("the first hint was ${step.deduction.technique}", step.deduction.technique.isCage)
    }

    @Test
    fun aHintOnAKillerIsStillTrue() {
        // Whatever it says, it must never place a digit the answer does not want, which is
        // the one way a hint can be worse than no hint.
        var state = killer()
        repeat(30) {
            val hint = HintEngine.next(state)
            if (hint !is Hint.Step) return
            for ((cell, digit) in hint.deduction.placements) {
                assertEquals("a hint placed the wrong digit", state.solution.atIndex(cell), digit)
            }
            state = state.applyHint(hint.deduction)
        }
    }

    @Test
    fun anOrdinaryPuzzleNeverGetsACageHint() {
        val plain = GameState.start(
            RatedPuzzle(
                puzzle = Puzzle(
                    givens = Board.parse(classic, realGivens),
                    solution = checkNotNull(com.sendoku.engine.Solver(classic).solve(Board.parse(classic, realGivens))),
                ),
                rating = 3.0,
                grade = com.sendoku.engine.Grade.TRICKY,
                hardest = null,
                symmetry = Symmetry.NONE,
                usage = emptyMap(),
            ),
        )
        val hint = HintEngine.next(plain)
        assertTrue(hint is Hint.Step)
        assertFalse((hint as Hint.Step).deduction.technique.isCage)
    }

    @Test
    fun aKillerIsCountedApartFromTheLadder() {
        // Graded on the same scale, and not a rung of the same ladder. A Steady Killer among
        // the Steady puzzles beaten would put a number on the climb that nobody climbed.
        val killerWin = FinishedGame(
            givens = "",
            grade = Grade.STEADY,
            rating = 2.5,
            hardest = null,
            elapsed = Duration.ZERO,
            hintsUsed = 0,
            mistakes = 0,
            solved = true,
            finishedAt = 1L,
            origin = PuzzleOrigin.KILLER,
        )
        val ladderWin = killerWin.copy(origin = PuzzleOrigin.LADDER, finishedAt = 2L)
        val stats = Statistics.of(listOf(killerWin, ladderWin))
        assertEquals(2, stats.totalSolved)
        assertEquals(1, stats.killerSolved)
        assertEquals("a Killer was counted as a rung of the ladder", 1, stats.byGrade.getValue(Grade.STEADY).solved)
    }

    @Test
    fun theShippedKillerBatchIsReadable() {
        val stream = KillerCatalogReader::class.java.getResourceAsStream("/catalog/killer.sdkk")
        if (stream == null) return
        val reader = stream.use { KillerCatalogReader.from(it) }
        assertTrue("the Killer batch is empty", reader.size > 0)
        for (index in 0 until minOf(reader.size, 10)) {
            val rated = reader.puzzleAt(index)
            // The sums are not stored, they are added up from the solution, so a decoded
            // puzzle that disagrees with its own cages would be caught here.
            for (cage in rated.puzzle.cages) {
                assertEquals(cage.sum, cage.cells.sumOf { rated.puzzle.solution.atIndex(it) })
            }
            assertEquals(classic.cellCount, rated.puzzle.cages.sumOf { it.size })
        }
    }
}
