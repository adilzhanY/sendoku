package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * The history, and the one thing it is for: finding a game again and sending it to somebody.
 *
 * Every one of these games was already being written down. What is tested here is that they
 * come back out, in an order a person can use, and that a game shared a week later is still
 * the game that was played rather than a grid rebuilt out of hope.
 */
class HistoryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    /** A real finished game, played to the end so the board it kept is a real one. */
    private fun won(at: Long): FinishedGame {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9_001))
        val puzzle = generateSequence { maker.next(Symmetry.ROTATIONAL) }.first()
        var state = GameState.start(puzzle)
        for (cell in state.cells.indices) {
            if (!state.cells[cell].isEmpty) continue
            state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        return FinishedGame.of(state, finishedAt = at)
    }

    @Test
    fun everyFinishedGameIsListedAndCanBeOpened() {
        val games = listOf(won(2_000L), won(1_000L))
        var opened: FinishedGame? = null
        compose.setContent {
            SendokuTheme {
                HistoryScreen(games = games, onBack = {}, onOpen = { opened = it })
            }
        }

        compose.onNodeWithTag("history:list").assertIsDisplayed()
        assertEquals(
            "the list did not show one row per game",
            games.size,
            compose.onAllNodesWithTag("history:game:2000").fetchSemanticsNodes().size +
                compose.onAllNodesWithTag("history:game:1000").fetchSemanticsNodes().size,
        )

        compose.onNodeWithTag("history:game:2000").performClick()
        compose.waitForIdle()
        assertEquals("the row opened the wrong game", 2_000L, opened?.finishedAt)
    }

    @Test
    fun anEmptyHistorySaysSoRatherThanShowingNothing() {
        compose.setContent {
            SendokuTheme {
                HistoryScreen(games = emptyList(), onBack = {}, onOpen = {})
            }
        }
        compose.onNodeWithText("Nothing yet").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithTag("history:list").fetchSemanticsNodes().size)
    }

    @Test
    fun aGameWithABoardShowsItAndOffersToShareIt() {
        compose.setContent {
            SendokuTheme {
                HistoryGameScreen(game = won(3_000L), onBack = {}, onLearn = {})
            }
        }
        compose.onNodeWithTag("game:board").assertIsDisplayed()
        compose.onNodeWithTag("history:share").assertIsDisplayed()
        assertEquals(
            "a game with a board should not be apologising for one",
            0,
            compose.onAllNodesWithTag("history:no-board").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aGameWithNoBoardSaysSoAndDoesNotOfferAPicture() {
        // A lost game from before the board column existed. There is nothing to draw and
        // nothing honest to share, so the screen says that rather than inventing a grid.
        val old = won(4_000L).copy(board = null, solved = false)
        compose.setContent {
            SendokuTheme {
                HistoryGameScreen(game = old, onBack = {}, onLearn = {})
            }
        }
        compose.onNodeWithTag("history:no-board").assertIsDisplayed()
        assertTrue(
            "an empty card was offered for a game nobody kept",
            compose.onAllNodesWithTag("history:share").fetchSemanticsNodes().isEmpty(),
        )
    }
}
