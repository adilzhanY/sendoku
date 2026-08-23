package com.sendoku.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.flow.Flow

/**
 * The one game in progress.
 *
 * A single row, overwritten. There is no notion of several games on the go at once, and
 * adding one later would be a new table rather than a change to this, because the point of
 * this row is that resuming never has to choose.
 */
@Entity(tableName = "in_progress")
public data class InProgressRow(
    @PrimaryKey val id: Int = ONLY_ROW,
    val givens: String,
    val solution: String,
    val entries: String,
    val marks: String,
    val grade: String,
    val rating: Double,
    val hardest: String?,
    val selected: Int?,
    val pencilMode: Boolean,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val savedAt: Long,
) {
    public companion object {
        public const val ONLY_ROW: Int = 1
    }
}

/** One finished game, kept for the statistics screen. */
@Entity(tableName = "finished")
public data class FinishedRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val givens: String,
    val grade: String,
    val rating: Double,
    val hardest: String?,
    val elapsedSeconds: Long,
    val hintsUsed: Int,
    val mistakes: Int,
    val solved: Boolean,
    val finishedAt: Long,
)

@Dao
public interface InProgressDao {

    @Query("SELECT * FROM in_progress WHERE id = :id")
    public suspend fun load(id: Int = InProgressRow.ONLY_ROW): InProgressRow?

    @Query("SELECT * FROM in_progress WHERE id = :id")
    public fun watch(id: Int = InProgressRow.ONLY_ROW): Flow<InProgressRow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun save(row: InProgressRow)

    @Query("DELETE FROM in_progress")
    public suspend fun clear()
}

@Dao
public interface FinishedDao {

    @Insert
    public suspend fun record(row: FinishedRow): Long

    @Query("SELECT * FROM finished ORDER BY finishedAt DESC")
    public fun watchAll(): Flow<List<FinishedRow>>

    @Query("SELECT * FROM finished ORDER BY finishedAt DESC LIMIT :limit")
    public suspend fun recent(limit: Int): List<FinishedRow>

    @Query("SELECT COUNT(*) FROM finished WHERE solved = 1")
    public fun watchSolvedCount(): Flow<Int>

    @Query("SELECT MIN(elapsedSeconds) FROM finished WHERE solved = 1 AND grade = :grade")
    public suspend fun bestSeconds(grade: String): Long?

    @Query("DELETE FROM finished")
    public suspend fun clear()
}

/**
 * The database.
 *
 * Schemas are exported to `app/schemas` and committed, which is what makes a migration
 * possible at all: without the old schema on disk there is nothing to migrate from. There
 * is deliberately no destructive fallback. A missing migration should crash a developer
 * before release rather than quietly delete a player's history after it.
 */
@Database(
    entities = [InProgressRow::class, FinishedRow::class],
    version = SendokuDatabase.VERSION,
    exportSchema = true,
)
public abstract class SendokuDatabase : RoomDatabase() {

    public abstract fun inProgress(): InProgressDao

    public abstract fun finished(): FinishedDao

    public companion object {
        public const val VERSION: Int = 1
        public const val NAME: String = "sendoku.db"
    }
}

/** Converts between the rows and the shapes the rest of the app uses. */
internal fun InProgressRow.toSaved(): SavedGame = SavedGame(
    givens = givens,
    solution = solution,
    entries = entries,
    marks = marks,
    grade = Grade.valueOf(grade),
    rating = rating,
    hardest = hardest?.let { TechniqueId.valueOf(it) },
    selected = selected,
    pencilMode = pencilMode,
    elapsed = elapsedSeconds.toDuration(),
    mistakes = mistakes,
    hintsUsed = hintsUsed,
)

internal fun SavedGame.toRow(savedAt: Long): InProgressRow = InProgressRow(
    givens = givens,
    solution = solution,
    entries = entries,
    marks = marks,
    grade = grade.name,
    rating = rating,
    hardest = hardest?.name,
    selected = selected,
    pencilMode = pencilMode,
    elapsedSeconds = elapsed.toSeconds(),
    mistakes = mistakes,
    hintsUsed = hintsUsed,
    savedAt = savedAt,
)

internal fun FinishedRow.toFinished(): FinishedGame = FinishedGame(
    givens = givens,
    grade = Grade.valueOf(grade),
    rating = rating,
    hardest = hardest?.let { TechniqueId.valueOf(it) },
    elapsed = elapsedSeconds.toDuration(),
    hintsUsed = hintsUsed,
    mistakes = mistakes,
    solved = solved,
    finishedAt = finishedAt,
)

internal fun FinishedGame.toRow(): FinishedRow = FinishedRow(
    givens = givens,
    grade = grade.name,
    rating = rating,
    hardest = hardest?.name,
    elapsedSeconds = elapsed.toSeconds(),
    hintsUsed = hintsUsed,
    mistakes = mistakes,
    solved = solved,
    finishedAt = finishedAt,
)
