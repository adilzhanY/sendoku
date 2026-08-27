package com.sendoku.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * Playing, from the outside.
 *
 * The rules are covered exhaustively by unit tests, which is where they belong. What these
 * cover is the wiring: that tapping a key really reaches the state, that undo really comes
 * back, and that finishing really shows the win panel. Every one of those is a connection
 * that a unit test cannot see.
 */
class GameFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle: RatedPuzzle by lazy {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9801))
        var made: RatedPuzzle? = null
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    /** The whole screen, driven the way the app drives it. */
    private fun play(start: GameState = GameState.start(puzzle)): () -> GameState {
        var state by mutableStateOf(start)
        compose.setContent {
            SendokuTheme {
                GameScreen(
                    state = state,
                    onEvent = { state = state.reduce(it) },
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = { _ -> },
                    onSettings = {},
                    onPath = {},
                    onSpend = { _, _ -> },
                )
            }
        }
        return { state }
    }

    @Test
    fun tappingACellThenAKeyPlacesTheDigit() {
        val start = GameState.start(puzzle)
        val current = play(start)
        val cell = (0 until 81).first { start.cells[it].isEmpty }
        val digit = start.solution.atIndex(cell)

        compose.onNodeWithContentDescription(describeFor(start, cell)).performClick()
        compose.onNodeWithContentDescription(padLabel(start, digit)).performClick()

        assertEquals(digit, current().cells[cell].digit)
    }

    @Test
    fun undoTakesBackWhatTheKeyPutIn() {
        val start = GameState.start(puzzle)
        val current = play(start)
        val cell = (0 until 81).first { start.cells[it].isEmpty }

        compose.onNodeWithContentDescription(describeFor(start, cell)).performClick()
        compose.onNodeWithContentDescription(padLabel(start, start.solution.atIndex(cell))).performClick()
        compose.onNodeWithText("Undo").performClick()

        assertTrue(current().cells[cell].isEmpty)
    }

    @Test
    fun undoIsOffUntilThereIsSomethingToUndo() {
        play()
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        compose.onNodeWithText("Redo").assertIsNotEnabled()
    }

    @Test
    fun notesModePencilsRatherThanPlaces() {
        val start = GameState.start(puzzle)
        val current = play(start)
        val cell = (0 until 81).first { start.cells[it].isEmpty }

        compose.onNodeWithText("Notes").performClick()
        compose.onNodeWithContentDescription(describeFor(start, cell)).performClick()
        compose.onNodeWithContentDescription(padLabel(start, 5, notes = true)).performClick()

        assertTrue(current().cells[cell].isEmpty)
        assertTrue(5 in current().cells[cell].marks)
    }

    @Test
    fun finishingShowsTheWinPanel() {
        var solved = GameState.start(puzzle)
        for (cell in 0 until 81) {
            if (solved.cells[cell].isEmpty) {
                solved = solved.select(cell).enter(solved.solution.atIndex(cell))
            }
        }
        play(solved)
        compose.onNodeWithText("SOLVED").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun askingForAHintShowsOneAndCanBeClosed() {
        // The button opens a chooser now. Two of the things on it are free, and the hint
        // itself is one deliberate tap further in.
        play()
        compose.onNodeWithText("Hint").performClick()
        compose.onNodeWithTag("hint:menu").assertIsDisplayed()
        compose.onNodeWithTag("hint:menu:explain").performClick()
        // A card of the deck, with the dots that say how far through it this one is.
        compose.onNodeWithTag("hint:dots").assertIsDisplayed()
        compose.onNodeWithTag("hint:close").performClick()
        compose.onNodeWithTag("hint:dots").assertDoesNotExistNow()
    }

    @Test
    fun whileHelpIsOnScreenThereIsNothingElseToPress() {
        // A hint is read with a thumb resting where the keys were, so leaving them live is a
        // wrong digit waiting to happen, and a wrong digit ends the explanation it was about
        // to follow. The board stops taking taps for the same reason: a hint describes the
        // board it was asked about.
        val state = play()
        val before = state()
        val cell = before.cells.indices.first { before.cells[it].isEmpty }

        compose.onNodeWithText("Hint").performClick()
        compose.waitForIdle()

        assertTrue("the number pad was still there", compose.missing("pad:1"))
        assertTrue("the tools were still there", compose.missing("tool:Notes"))
        compose.onNodeWithTag("game:cell:$cell").performClick()
        compose.waitForIdle()
        assertEquals("the board took a tap while a hint was up", before.selected, state().selected)

        compose.onNodeWithTag("hint:menu:close").performClick()
        compose.waitForIdle()
        assertTrue("the number pad did not come back", !compose.missing("pad:1"))
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.missing(tag: String): Boolean =
        onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()

    @Test
    fun askingWhetherAnythingIsWrongCostsNoHint() {
        val state = play()
        compose.onNodeWithText("Hint").performClick()
        compose.onNodeWithTag("hint:menu:check").performClick()

        compose.onNodeWithTag("hint:check").assertIsDisplayed()
        assertEquals("a free question was charged for", 0, state().hintsUsed)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistNow() {
        try {
            assertIsDisplayed()
            error("the hint panel is still on screen")
        } catch (expected: AssertionError) {
            // Gone, which is what was wanted.
        }
    }

    private fun describeFor(state: GameState, cell: Int): String {
        val row = cell / 9 + 1
        val column = cell % 9 + 1
        val position = "Row $row, column $column"
        val target = state.cells[cell]
        return when {
            target.isGiven -> "$position, ${target.digit}, a clue"
            !target.isEmpty -> "$position, ${target.digit}"
            target.marks.isNotEmpty -> "$position, empty, noted ${target.marks.toList().joinToString(", ")}"
            else -> "$position, empty"
        }
    }

    private fun padLabel(state: GameState, digit: Int, notes: Boolean = false): String {
        val remaining = state.remaining(digit)
        val base = when {
            remaining <= 0 -> "$digit, all placed"
            remaining == 1 -> "$digit, one left"
            else -> "$digit, $remaining left"
        }
        return if (notes) "$base, notes mode" else base
    }

    @Test
    fun hintOffersToTakeAWrongDigitBackOffTheBoard() {
        val start = GameState.start(puzzle)
        val empty = start.cells.indices.first { start.cells[it].isEmpty }
        val right = start.solution.atIndex(empty)
        val wrong = (1..9).first { it != right }
        val state = play(start.select(empty).enter(wrong))

        compose.onNodeWithText("Hint", ignoreCase = true).performClick()
        compose.onNodeWithTag("hint:menu:explain").performClick()
        compose.onNodeWithText("Take it off", ignoreCase = true).assertIsDisplayed().performClick()

        assertTrue(state().cells[empty].isEmpty)
    }

    @Test
    fun eraseIsDeadUntilTheSelectedCellHoldsSomething() {
        val start = GameState.start(puzzle)
        val empty = start.cells.indices.first { start.cells[it].isEmpty }
        play(start.select(empty))

        compose.onNodeWithText("Erase", ignoreCase = true).assertIsNotEnabled()
    }
}
