package com.sendoku.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
    /** The day this puzzle belongs to, when it came from the calendar rather than the ladder. */
    val dailyEpochDay: Long? = null,
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
    /**
     * The board as it was left, or null for a game finished before this column existed.
     *
     * Kept so a game can be looked at and shared long after the ten seconds the result panel
     * is on screen. A won game could be rebuilt from the givens by solving them again, and is
     * when this is null, but a lost one could not: what was on the board when it ended is not
     * derivable from anything else, and it is the half of the history worth keeping.
     */
    val board: String? = null,
    /**
     * Which day's puzzle this was, or null for one off the ladder.
     *
     * The day, not the timestamp. A player in Auckland who solves Tuesday's puzzle is on
     * Tuesday's puzzle whatever the clock says in London, and the calendar has to mark the
     * square they played rather than the square their timezone happens to fall in.
     */
    val dailyEpochDay: Long? = null,
)

/**
 * How far through a lesson somebody is.
 *
 * One row per lesson they have opened. The step is where they left off, so a lesson closed
 * halfway reopens halfway rather than at the beginning, which is the difference between a
 * course you can do on a bus and one you have to sit down for.
 */
@Entity(tableName = "lesson_progress")
public data class LessonProgressRow(
    @PrimaryKey val lessonId: String,
    val step: Int,
    val finished: Boolean,
    val lastSeenAt: Long,
)

/**
 * How well a technique is known.
 *
 * Counted per technique rather than per lesson, because a lesson is read once and a technique
 * is practised. [correct] is the run of correct answers since the last wrong one or the last
 * reveal, which is what the mastery rule reads.
 */
@Entity(tableName = "technique_mastery")
public data class TechniqueMasteryRow(
    @PrimaryKey val technique: String,
    val attempts: Int,
    val correct: Int,
    val streak: Int,
    val mastered: Boolean,
    val lastPractisedAt: Long,
)

@Dao
public interface LessonProgressDao {

    @Query("SELECT * FROM lesson_progress")
    public fun watchAll(): Flow<List<LessonProgressRow>>

    @Query("SELECT * FROM lesson_progress")
    public suspend fun all(): List<LessonProgressRow>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :id")
    public suspend fun of(id: String): LessonProgressRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun save(row: LessonProgressRow)

    @Query("DELETE FROM lesson_progress")
    public suspend fun clear()
}

@Dao
public interface TechniqueMasteryDao {

    @Query("SELECT * FROM technique_mastery")
    public fun watchAll(): Flow<List<TechniqueMasteryRow>>

    @Query("SELECT * FROM technique_mastery")
    public suspend fun all(): List<TechniqueMasteryRow>

    @Query("SELECT * FROM technique_mastery WHERE technique = :id")
    public suspend fun of(id: String): TechniqueMasteryRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun save(row: TechniqueMasteryRow)

    @Query("DELETE FROM technique_mastery")
    public suspend fun clear()
}

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

    @Query("SELECT * FROM finished ORDER BY finishedAt DESC")
    public suspend fun all(): List<FinishedRow>

    @Query("SELECT * FROM finished ORDER BY finishedAt DESC LIMIT :limit")
    public suspend fun recent(limit: Int): List<FinishedRow>

    @Query("SELECT COUNT(*) FROM finished WHERE solved = 1")
    public fun watchSolvedCount(): Flow<Int>

    @Query("SELECT MIN(elapsedSeconds) FROM finished WHERE solved = 1 AND grade = :grade")
    public suspend fun bestSeconds(grade: String): Long?

    /** Which days have been solved, for marking up the calendar. */
    @Query("SELECT DISTINCT dailyEpochDay FROM finished WHERE solved = 1 AND dailyEpochDay IS NOT NULL")
    public fun watchSolvedDays(): Flow<List<Long>>

    /** Which days have been played but not finished. */
    @Query("SELECT DISTINCT dailyEpochDay FROM finished WHERE solved = 0 AND dailyEpochDay IS NOT NULL")
    public fun watchAttemptedDays(): Flow<List<Long>>

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
    entities = [
        InProgressRow::class,
        FinishedRow::class,
        LessonProgressRow::class,
        TechniqueMasteryRow::class,
    ],
    version = SendokuDatabase.VERSION,
    exportSchema = true,
)
public abstract class SendokuDatabase : RoomDatabase() {

    public abstract fun inProgress(): InProgressDao

    public abstract fun finished(): FinishedDao

    public abstract fun lessonProgress(): LessonProgressDao

    public abstract fun mastery(): TechniqueMasteryDao

    public companion object {
        public const val VERSION: Int = 4
        public const val NAME: String = "sendoku.db"

        /**
         * Version 1 to 2: which day a puzzle belonged to.
         *
         * Nullable and with no default, because every game already recorded was played off
         * the ladder rather than from the calendar, and saying otherwise would put marks on a
         * calendar the player never opened.
         */
        public val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE finished ADD COLUMN dailyEpochDay INTEGER")
                connection.execSQL("ALTER TABLE in_progress ADD COLUMN dailyEpochDay INTEGER")
            }
        }

        /**
         * Version 2 to 3: the learning course.
         *
         * Two new tables and nothing touched. Course progress lives in the same database as
         * everything else on purpose, so one backup covers a player's whole history and there
         * is no second file to lose.
         */
        public val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lesson_progress` (" +
                        "`lessonId` TEXT NOT NULL, `step` INTEGER NOT NULL, " +
                        "`finished` INTEGER NOT NULL, `lastSeenAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`lessonId`))",
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `technique_mastery` (" +
                        "`technique` TEXT NOT NULL, `attempts` INTEGER NOT NULL, " +
                        "`correct` INTEGER NOT NULL, `streak` INTEGER NOT NULL, " +
                        "`mastered` INTEGER NOT NULL, `lastPractisedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`technique`))",
                )
            }
        }

        /**
         * Version 3 to 4: the board a finished game was left on.
         *
         * Nullable and with no default, for the same reason the day was: every game already
         * recorded was finished before anything kept the board, and inventing one would put a
         * grid on the screen that the player never played.
         */
        public val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE finished ADD COLUMN board TEXT")
            }
        }

        public val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    dailyEpochDay = dailyEpochDay,
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
    dailyEpochDay = dailyEpochDay,
)

internal fun FinishedRow.toFinished(): FinishedGame = FinishedGame(
    givens = givens,
    board = board,
    grade = Grade.valueOf(grade),
    rating = rating,
    hardest = hardest?.let { TechniqueId.valueOf(it) },
    elapsed = elapsedSeconds.toDuration(),
    hintsUsed = hintsUsed,
    mistakes = mistakes,
    solved = solved,
    finishedAt = finishedAt,
    dailyEpochDay = dailyEpochDay,
)

internal fun FinishedGame.toRow(): FinishedRow = FinishedRow(
    givens = givens,
    board = board,
    grade = grade.name,
    rating = rating,
    hardest = hardest?.name,
    elapsedSeconds = elapsed.toSeconds(),
    hintsUsed = hintsUsed,
    mistakes = mistakes,
    solved = solved,
    finishedAt = finishedAt,
    dailyEpochDay = dailyEpochDay,
)
