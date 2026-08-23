package com.sendoku.app.data

import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The repository, over stand in DAOs.
 *
 * Room's DAOs are interfaces, which means the logic above them can be tested for real
 * without a database or a device. What is not covered here is Room's own generated code:
 * that needs an emulator, and the schema guard next door is what watches it instead.
 */
class GameRepositoryTest {

    private class FakeInProgress : InProgressDao {
        val rows = MutableStateFlow<InProgressRow?>(null)
        override suspend fun load(id: Int): InProgressRow? = rows.value
        override fun watch(id: Int): Flow<InProgressRow?> = rows
        override suspend fun save(row: InProgressRow) { rows.value = row }
        override suspend fun clear() { rows.value = null }
    }

    private class FakeFinished : FinishedDao {
        val rows = MutableStateFlow<List<FinishedRow>>(emptyList())
        private var nextId = 1L
        override suspend fun record(row: FinishedRow): Long {
            val id = nextId++
            rows.value = rows.value + row.copy(id = id)
            return id
        }
        override fun watchAll(): Flow<List<FinishedRow>> = rows.map { it.sortedByDescending { r -> r.finishedAt } }
        override suspend fun recent(limit: Int): List<FinishedRow> = rows.value.takeLast(limit)
        override fun watchSolvedCount(): Flow<Int> = rows.map { list -> list.count { it.solved } }
        override suspend fun bestSeconds(grade: String): Long? =
            rows.value.filter { it.solved && it.grade == grade }.minOfOrNull { it.elapsedSeconds }
        override suspend fun clear() { rows.value = emptyList() }
    }

    private val puzzle: RatedPuzzle by lazy {
        var made: RatedPuzzle? = null
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(9301))
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    private fun played(): GameState {
        var state = GameState.start(puzzle)
        val at = state.cells.indices.first { state.cells[it].isEmpty }
        return state.select(at).enter(state.solution.atIndex(at)).tick(3.minutes)
    }

    @Test
    fun `nothing saved means nothing to resume`() = runTest {
        val repository = RoomGameRepository(FakeInProgress(), FakeFinished())
        assertNull(repository.loadInProgress(GameSettings()))
    }

    @Test
    fun `a saved game comes back the way it went in`() = runTest {
        val repository = RoomGameRepository(FakeInProgress(), FakeFinished())
        val before = played()
        repository.saveInProgress(before)

        val after = repository.loadInProgress(before.settings)
        assertEquals(before.cells, after?.cells)
        assertEquals(before.elapsed, after?.elapsed)
        assertEquals(before.grade, after?.grade)
    }

    @Test
    fun `saving twice overwrites rather than piling up`() = runTest {
        val dao = FakeInProgress()
        val repository = RoomGameRepository(dao, FakeFinished())
        repository.saveInProgress(played())
        repository.saveInProgress(played().tick(1.minutes))
        assertEquals(InProgressRow.ONLY_ROW, dao.rows.value?.id)
    }

    @Test
    fun `a finished game is never left sitting there as resumable`() = runTest {
        val dao = FakeInProgress()
        val repository = RoomGameRepository(dao, FakeFinished())
        repository.saveInProgress(played())

        var solved = GameState.start(puzzle)
        for (at in 0 until 81) {
            if (solved.cells[at].isEmpty) solved = solved.select(at).enter(solved.solution.atIndex(at))
        }
        repository.saveInProgress(solved)

        assertNull(dao.rows.value)
        assertNull(repository.loadInProgress(GameSettings()))
    }

    @Test
    fun `finishing files the game and clears the one in progress`() = runTest {
        val inProgress = FakeInProgress()
        val finished = FakeFinished()
        val repository = RoomGameRepository(inProgress, finished)

        repository.saveInProgress(played())
        repository.recordFinished(played(), finishedAt = 4242L)

        assertNull(inProgress.rows.value)
        val history = repository.history().first()
        assertEquals(1, history.size)
        assertEquals(4242L, history.first().finishedAt)
        assertEquals(puzzle.grade, history.first().grade)
    }

    @Test
    fun `history comes back newest first`() = runTest {
        val repository = RoomGameRepository(FakeInProgress(), FakeFinished())
        repository.recordFinished(played(), finishedAt = 100L)
        repository.recordFinished(played(), finishedAt = 300L)
        repository.recordFinished(played(), finishedAt = 200L)

        assertEquals(listOf(300L, 200L, 100L), repository.history().first().map { it.finishedAt })
    }

    @Test
    fun `solved counts are grouped by grade and ignore the ones that were lost`() = runTest {
        val finished = FakeFinished()
        val repository = RoomGameRepository(FakeInProgress(), finished)

        repository.recordFinished(solvedGame(), finishedAt = 1L)
        repository.recordFinished(solvedGame(), finishedAt = 2L)
        repository.recordFinished(played(), finishedAt = 3L)

        val counts = repository.solvedByGrade().first()
        assertEquals(2, counts[puzzle.grade])
        assertEquals(3, finished.rows.value.size)
    }

    @Test
    fun `clearing the game in progress leaves the history alone`() = runTest {
        val repository = RoomGameRepository(FakeInProgress(), FakeFinished())
        repository.recordFinished(solvedGame(), finishedAt = 1L)
        repository.saveInProgress(played())

        repository.clearInProgress()

        assertNull(repository.loadInProgress(GameSettings()))
        assertTrue(repository.history().first().isNotEmpty())
    }

    private fun solvedGame(): GameState {
        var state = GameState.start(puzzle)
        for (at in 0 until 81) {
            if (state.cells[at].isEmpty) state = state.select(at).enter(state.solution.atIndex(at))
        }
        return state
    }
}
