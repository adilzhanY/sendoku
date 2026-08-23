package com.sendoku.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
                    onGlossary = {},
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
        play()
        compose.onNodeWithText("Hint").performClick()
        compose.onNodeWithText("Show me where").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Show me where").assertDoesNotExistNow()
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
}
