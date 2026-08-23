package com.sendoku.app.data

import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.engine.Grade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Everything the app stores, behind one door.
 *
 * The rest of the app never sees a Room row or a preferences key. That is worth the extra
 * type: it keeps the storage choice reversible, and it means the parts with real logic in
 * them can be tested against a stand in rather than a database.
 */
public interface GameRepository {

    /** The game the player left, if there is one. */
    public suspend fun loadInProgress(settings: GameSettings): GameState?

    public suspend fun saveInProgress(state: GameState)

    public suspend fun clearInProgress()

    /** Files a finished game and clears the one in progress, since it is the same game. */
    public suspend fun recordFinished(state: GameState, finishedAt: Long)

    public fun history(): Flow<List<FinishedGame>>

    /** How many puzzles of each grade the player has solved. */
    public fun solvedByGrade(): Flow<Map<Grade, Int>>
}

/** The real one, over Room. */
public class RoomGameRepository(
    private val inProgress: InProgressDao,
    private val finished: FinishedDao,
) : GameRepository {

    override suspend fun loadInProgress(settings: GameSettings): GameState? =
        inProgress.load()?.toSaved()?.toState(settings)

    override suspend fun saveInProgress(state: GameState) {
        // A finished game is history, not something to resume into.
        if (state.isOver) {
            inProgress.clear()
            return
        }
        inProgress.save(SavedGame.of(state).toRow(savedAt = state.elapsed.toSeconds()))
    }

    override suspend fun clearInProgress() {
        inProgress.clear()
    }

    override suspend fun recordFinished(state: GameState, finishedAt: Long) {
        finished.record(FinishedGame.of(state, finishedAt).toRow())
        inProgress.clear()
    }

    override fun history(): Flow<List<FinishedGame>> =
        finished.watchAll().map { rows -> rows.map { it.toFinished() } }

    override fun solvedByGrade(): Flow<Map<Grade, Int>> =
        finished.watchAll().map { rows ->
            rows.filter { it.solved }
                .groupingBy { Grade.valueOf(it.grade) }
                .eachCount()
        }
}
