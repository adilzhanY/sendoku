package com.sendoku.app.game

import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.GameRepository
import com.sendoku.app.data.PuzzleSource
import com.sendoku.app.data.SettingsStore
import com.sendoku.app.ui.GameEvent
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val puzzles: List<RatedPuzzle> by lazy {
        // next() returns null for a grid the ladder cannot rate, and generateSequence stops
        // dead on the first null, so the nulls have to be skipped rather than sequenced.
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9401))
        val made = ArrayList<RatedPuzzle>()
        while (made.size < 4) maker.next(Symmetry.ROTATIONAL)?.let(made::add)
        made
    }

    /** A repository that remembers, so a "new process" can be handed the same one. */
    private class MemoryRepository : GameRepository {
        var saved: GameState? = null
        var saveCount = 0
        val finished = MutableStateFlow<List<FinishedGame>>(emptyList())

        override suspend fun loadInProgress(settings: GameSettings): GameState? = saved?.copy(settings = settings)

        override suspend fun saveInProgress(state: GameState) {
            saveCount++
            saved = if (state.isOver) null else state
        }

        override fun watchInProgress(): Flow<com.sendoku.app.data.SavedGame?> =
            MutableStateFlow(saved?.let { com.sendoku.app.data.SavedGame.of(it) })

        override suspend fun clearInProgress() {
            saved = null
        }

        override fun dailyDays(): Flow<com.sendoku.app.data.DailyDays> = finished.map { games ->
            com.sendoku.app.data.DailyDays(
                solved = games.filter { it.solved }.mapNotNull { it.dailyEpochDay }.toSet(),
                attempted = games.filterNot { it.solved }.mapNotNull { it.dailyEpochDay }.toSet(),
            )
        }

        override suspend fun recordFinished(state: GameState, finishedAt: Long) {
            finished.value = finished.value + FinishedGame.of(state, finishedAt)
            saved = null
        }

        override fun history(): Flow<List<FinishedGame>> = finished

        override fun solvedByGrade(): Flow<Map<Grade, Int>> =
            finished.map { list -> list.filter { it.solved }.groupingBy { it.grade }.eachCount() }

        override fun statistics(): Flow<com.sendoku.app.data.Statistics> =
            finished.map { com.sendoku.app.data.Statistics.of(it) }

        override suspend fun clearHistory() {
            finished.value = emptyList()
        }
    }

    private class FixedSettings(settings: GameSettings = GameSettings()) : SettingsStore {
        val flow = MutableStateFlow(settings)
        private val look = MutableStateFlow(com.sendoku.app.data.Appearance())
        override val settings: Flow<GameSettings> = flow
        override val appearance: Flow<com.sendoku.app.data.Appearance> = look
        override suspend fun update(transform: (GameSettings) -> GameSettings) {
            flow.value = transform(flow.value)
        }
        override suspend fun updateAppearance(
            transform: (com.sendoku.app.data.Appearance) -> com.sendoku.app.data.Appearance,
        ) {
            look.value = transform(look.value)
        }
    }

    /**
     * A scope for the view model, on the test scheduler.
     *
     * Not `backgroundScope`: under the Android unit test runner its coroutines are never
     * dispatched by advanceUntilIdle, so every one of these tests silently observed a view
     * model that had not started. An explicit scope over the same scheduler behaves.
     */
    private fun TestScope.viewModelScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())

    private inner class RotatingPuzzles : PuzzleSource {
        var handedOut = 0
        override suspend fun next(grade: Grade): RatedPuzzle = puzzles[handedOut++ % puzzles.size]
        override suspend fun daily(epochDay: Long): RatedPuzzle = puzzles[(epochDay % puzzles.size).toInt()]
    }

    @Test
    fun `starting with nothing saved deals a new puzzle`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())

        model.resumeOrStart()
        advanceUntilIdle()

        assertNotNull(model.state.value)
        assertTrue(!model.loading.value)
    }

    @Test
    fun `a game in progress is picked up rather than replaced`() = runTest {
        val repository = MemoryRepository()
        val first = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())
        first.resumeOrStart()
        advanceUntilIdle()

        val at = first.state.value!!.cells.indices.first { first.state.value!!.cells[it].isEmpty }
        val digit = first.state.value!!.solution.atIndex(at)
        first.onEvent(GameEvent.Select(at))
        first.onEvent(GameEvent.Digit(digit))
        advanceUntilIdle()

        // A brand new view model, as though the process had been killed and restarted.
        val second = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())
        second.resumeOrStart()
        advanceUntilIdle()

        assertEquals(digit, second.state.value!!.cells[at].digit)
        assertEquals(first.state.value!!.cells, second.state.value!!.cells)
    }

    @Test
    fun `a burst of moves is written down once, not once each`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()
        val before = repository.saveCount

        val empties = model.state.value!!.cells.indices.filter { model.state.value!!.cells[it].isEmpty }
        for (at in empties.take(6)) {
            model.onEvent(GameEvent.Select(at))
            model.onEvent(GameEvent.Digit(model.state.value!!.solution.atIndex(at)))
            advanceTimeBy(50)
        }
        advanceUntilIdle()

        assertEquals("six moves in quick succession should cost one write", 1, repository.saveCount - before)
    }

    @Test
    fun `two bursts separated by a pause are written down twice`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()
        val before = repository.saveCount

        val empties = model.state.value!!.cells.indices.filter { model.state.value!!.cells[it].isEmpty }
        model.onEvent(GameEvent.Select(empties[0]))
        advanceUntilIdle()
        model.onEvent(GameEvent.Select(empties[1]))
        advanceUntilIdle()

        assertEquals(2, repository.saveCount - before)
    }

    @Test
    fun `saving now does not wait for the pause`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()

        val at = model.state.value!!.cells.indices.first { model.state.value!!.cells[it].isEmpty }
        model.onEvent(GameEvent.Select(at))
        model.saveNow()
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(at, repository.saved?.selected)
    }

    @Test
    fun `finishing files the game and leaves nothing to resume`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope()) { 777L }
        model.resumeOrStart()
        advanceUntilIdle()

        var state = model.state.value!!
        for (at in 0 until 81) {
            if (state.cells[at].isEmpty) {
                model.onEvent(GameEvent.Select(at))
                model.onEvent(GameEvent.Digit(state.solution.atIndex(at)))
                state = model.state.value!!
            }
        }
        advanceUntilIdle()

        assertTrue(model.state.value!!.isSolved)
        assertEquals(1, repository.finished.value.size)
        assertEquals(777L, repository.finished.value.first().finishedAt)
        assertNull(repository.saved)
    }

    @Test
    fun `a game is filed once, however many events arrive after it ends`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(mistakeLimit()), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()

        val state = model.state.value!!
        val at = state.cells.indices.first { state.cells[it].isEmpty }
        model.onEvent(GameEvent.Select(at))
        model.onEvent(GameEvent.Digit((1..9).first { it != state.solution.atIndex(at) }))
        advanceUntilIdle()

        assertTrue(model.state.value!!.isOver)
        model.onEvent(GameEvent.Digit(1))
        model.onEvent(GameEvent.Undo)
        advanceUntilIdle()

        assertEquals(1, repository.finished.value.size)
    }

    @Test
    fun `starting a new game replaces the current one`() = runTest {
        val repository = MemoryRepository()
        val puzzleSource = RotatingPuzzles()
        val model = GameViewModel(repository, FixedSettings(), puzzleSource, viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()
        val first = model.state.value!!

        model.startNew(Grade.GENTLE)
        advanceUntilIdle()

        assertEquals(2, puzzleSource.handedOut)
        assertTrue(model.state.value!!.solution != first.solution || puzzles.size == 1)
    }

    @Test
    fun `settings from the store reach the game`() = runTest {
        val strict = GameSettings(flagConflicts = false, showTimer = false, mistakeLimit = 5)
        val model = GameViewModel(MemoryRepository(), FixedSettings(strict), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()
        assertEquals(strict, model.state.value!!.settings)
    }

    @Test
    fun `pausing stops the clock and writes the game down`() = runTest {
        val repository = MemoryRepository()
        val model = GameViewModel(repository, FixedSettings(), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()

        model.pause()
        advanceUntilIdle()

        assertTrue(!model.state.value!!.isRunning)
        assertNotNull(repository.saved)
        assertTrue(!repository.saved!!.isRunning)
    }

    @Test
    fun `the daily is the same puzzle however many times it is opened`() = runTest {
        val source = RotatingPuzzles()
        val model = GameViewModel(MemoryRepository(), FixedSettings(), source, viewModelScope())

        model.startDaily(20_688L)
        advanceUntilIdle()
        val first = model.state.value!!.solution

        model.startDaily(20_688L)
        advanceUntilIdle()
        assertEquals(first, model.state.value!!.solution)
    }

    @Test
    fun `a different day is a different puzzle`() = runTest {
        val source = RotatingPuzzles()
        val model = GameViewModel(MemoryRepository(), FixedSettings(), source, viewModelScope())

        model.startDaily(20_688L)
        advanceUntilIdle()
        val sunday = model.state.value!!.solution

        model.startDaily(20_689L)
        advanceUntilIdle()
        assertTrue(sunday != model.state.value!!.solution)
    }

    @Test
    fun `turning the phone does not leave the game paused`() = runTest {
        val model = GameViewModel(MemoryRepository(), FixedSettings(), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()

        // What a rotation looks like from here: the activity stops, then a new one starts.
        model.onBackground()
        advanceUntilIdle()
        assertTrue(!model.state.value!!.isRunning)

        model.onForeground()
        advanceUntilIdle()
        assertTrue(model.state.value!!.isRunning)
    }

    @Test
    fun `a pause the player asked for survives coming back`() = runTest {
        val model = GameViewModel(MemoryRepository(), FixedSettings(), RotatingPuzzles(), viewModelScope())
        model.resumeOrStart()
        advanceUntilIdle()

        model.pause()
        model.onBackground()
        model.onForeground()
        advanceUntilIdle()

        assertTrue(!model.state.value!!.isRunning)
    }

    private fun mistakeLimit() = GameSettings(mistakeLimit = 1)
}
