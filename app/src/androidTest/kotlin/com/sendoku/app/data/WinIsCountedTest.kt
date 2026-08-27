package com.sendoku.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.ui.HomeState
import com.sendoku.app.ui.highestOpen
import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.CatalogReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Winning a puzzle has to count, all the way through to the two places that show it.
 *
 * Somebody finished an Easy puzzle, saw nought solved on their own page, and found the next
 * level still shut. Both of those read from the same row in the same table, so this walks the
 * whole path on a real database: play a real puzzle to the last digit, write it down, and
 * read it back the way the two screens do.
 */
@RunWith(AndroidJUnit4::class)
class WinIsCountedTest {

    private lateinit var database: SendokuDatabase
    private lateinit var repository: RoomGameRepository

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SendokuDatabase::class.java,
        ).build()
        repository = RoomGameRepository(database.inProgress(), database.finished())
    }

    @After
    fun close() {
        database.close()
    }

    private fun easy(): GameState {
        val stream = InstrumentationRegistry.getInstrumentation().targetContext.assets
            .runCatching { open("catalog/classic.sdkb") }
            .getOrElse { checkNotNull(javaClass.classLoader?.getResourceAsStream("catalog/classic.sdkb")) }
        val catalog = CatalogReader.from(stream)
        return GameState.start(catalog.puzzleAt(catalog.indicesOf(Grade.GENTLE).first()))
    }

    @Test
    fun finishingAnEasyPuzzleCountsAndOpensTheNextLevel() = runBlocking {
        var state = easy()
        repository.saveInProgress(state)

        for (cell in state.cells.indices) {
            if (!state.cells[cell].isEmpty) continue
            state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        assertTrue("the board was not finished", state.isSolved)

        repository.recordFinished(state, finishedAt = 1_000L)

        assertEquals("the account page would still say nought", 1, repository.statistics().first().totalSolved)
        val counts = repository.solvedByGrade().first()
        assertEquals("the win was filed under the wrong level", mapOf(Grade.GENTLE to 1), counts)
        assertEquals(
            "one win did not open the next level",
            Grade.STEADY,
            HomeState(solvedByGrade = counts, inProgress = null).highestOpen(),
        )
        assertEquals("the finished game was left sitting in progress", null, repository.watchInProgress().first())
    }

    @Test
    fun theBoardIsKeptSoTheGameCanBeLookedAtLater() = runBlocking {
        var state = easy()
        for (cell in state.cells.indices) {
            if (!state.cells[cell].isEmpty) continue
            state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        repository.recordFinished(state, finishedAt = 3_000L)

        val recorded = repository.history().first().single()
        val replayed = checkNotNull(recorded.replay()) { "a game recorded today could not be rebuilt" }
        assertEquals(
            "the board that came back is not the board that was played",
            state.cells.map { it.digit },
            replayed.cells.map { it.digit },
        )
        assertEquals(
            "the clues came back as the player's own digits",
            state.cells.map { it.isGiven },
            replayed.cells.map { it.isGiven },
        )
    }

    @Test
    fun aWonGameFromBeforeTheColumnStillHasABoard() = runBlocking {
        // Every game finished before the board column existed has none, and a won board is
        // the solution, so it can be rebuilt by solving the puzzle again. This is what a
        // player with a year of history sees on the day they update.
        var state = easy()
        for (cell in state.cells.indices) {
            if (!state.cells[cell].isEmpty) continue
            state = state.select(cell).enter(state.solution.atIndex(cell))
        }
        repository.recordFinished(state, finishedAt = 4_000L)
        val old = repository.history().first().single().copy(board = null)

        val replayed = checkNotNull(old.replay()) { "an old won game could not be rebuilt" }
        assertEquals(
            "the rebuilt board is not the one that was solved",
            state.cells.map { it.digit },
            replayed.cells.map { it.digit },
        )
    }

    @Test
    fun aLostGameFromBeforeTheColumnSaysSoRatherThanInventingOne() = runBlocking {
        var state = easy().withSettings(GameSettings(mistakeLimit = 3))
        repeat(3) {
            val cell = state.cells.indices.first { at ->
                state.cells[at].isEmpty && state.solution.atIndex(at) in state.candidatesAt(at)
            }
            val wrong = (1..9).first { it != state.solution.atIndex(cell) }
            state = state.select(cell).enter(wrong).select(cell).erase()
        }
        repository.recordFinished(state, finishedAt = 5_000L)
        val old = repository.history().first().single().copy(board = null)

        // Nothing anywhere knows what was on that board, and drawing the solution instead
        // would be showing the player a grid they never played.
        assertEquals("a board was invented for a lost game that never kept one", null, old.replay())
    }

    @Test
    fun aLostGameCountsAsPlayedAndOpensNothing() = runBlocking {
        var state = easy().withSettings(GameSettings(mistakeLimit = 3))
        // Three real mistakes, in cells that were still healthy when they were made.
        repeat(3) {
            val cell = state.cells.indices.first { at ->
                state.cells[at].isEmpty && state.solution.atIndex(at) in state.candidatesAt(at)
            }
            val wrong = (1..9).first { it != state.solution.atIndex(cell) }
            state = state.select(cell).enter(wrong).select(cell).erase()
        }
        assertTrue("three mistakes did not end the game", state.isFailed)

        repository.recordFinished(state, finishedAt = 2_000L)

        assertEquals("a lost game was counted as solved", 0, repository.statistics().first().totalSolved)
        assertEquals(1, database.finished().all().size)
        assertEquals(emptyMap<Grade, Int>(), repository.solvedByGrade().first())
    }
}
