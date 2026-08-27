package com.sendoku.app.learn

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A player's record, as a file.
 *
 * There is no account and no server, so the only way to move a record to a new phone is to
 * hand the player the file. That is the whole feature: an export they can put in a password
 * manager, email to themselves, or keep on a stick, and an import that puts it back.
 *
 * The format is json because a person should be able to open it and see what it says about
 * them. An app that promises it collects nothing has to be willing to show its working, and a
 * binary blob would undo that in one step.
 */
@Serializable
public data class CourseBackup(
    /** The shape of this file. Bumped when a field changes meaning, never for an added field. */
    val format: Int = FORMAT,
    /** Which build wrote it. Only ever read by a human working out what went wrong. */
    val app: String = "",
    val writtenAt: Long = 0,
    val lessons: List<LessonRecord> = emptyList(),
    val mastery: List<MasteryRecord> = emptyList(),
    val games: List<GameRecord> = emptyList(),
) {
    public companion object {
        /**
         * Version one.
         *
         * A reader must refuse anything higher, since a file from a newer app can mean things
         * this one would misread, and accept anything lower it still understands.
         */
        public const val FORMAT: Int = 1

        private val json = Json {
            prettyPrint = true
            // A file written by a later version will have fields this one does not know. Those
            // are skipped rather than thrown at the player, which is what makes an added field
            // a compatible change.
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        public fun encode(backup: CourseBackup): String = json.encodeToString(backup)

        /**
         * Reads a file, or says why it will not.
         *
         * Every failure here is somebody's real file, so none of them throw. A truncated
         * download, a text file picked by mistake and a backup from a future version are three
         * different problems and the player is told which one they have.
         */
        public fun decode(text: String): BackupResult {
            if (text.isBlank()) return BackupResult.Unreadable(Problem.EMPTY)
            val parsed = try {
                json.decodeFromString<CourseBackup>(text)
            } catch (_: SerializationException) {
                return BackupResult.Unreadable(Problem.NOT_OURS)
            } catch (_: IllegalArgumentException) {
                return BackupResult.Unreadable(Problem.NOT_OURS)
            }
            if (parsed.format <= 0) return BackupResult.Unreadable(Problem.NOT_OURS)
            if (parsed.format > FORMAT) return BackupResult.Unreadable(Problem.FROM_THE_FUTURE)
            return BackupResult.Read(parsed)
        }
    }
}

@Serializable
public data class LessonRecord(val id: String, val step: Int, val finished: Boolean, val at: Long = 0)

@Serializable
public data class MasteryRecord(
    val technique: String,
    val attempts: Int,
    val correct: Int,
    val streak: Int,
    val mastered: Boolean,
    val at: Long = 0,
)

@Serializable
public data class GameRecord(
    val givens: String,
    /**
     * The board as it was left, when the version that wrote the file kept one.
     *
     * Optional and defaulted, so a file from before this existed still imports and a file
     * written now still opens in a build that has never heard of it.
     */
    val board: String? = null,
    val grade: String,
    val rating: Double,
    val hardest: String? = null,
    val seconds: Long,
    val hints: Int,
    val mistakes: Int,
    val solved: Boolean,
    val finishedAt: Long,
    val dailyEpochDay: Long? = null,
)

/** Why a file could not be read, in terms a player can act on. */
public enum class Problem { EMPTY, NOT_OURS, FROM_THE_FUTURE }

public sealed interface BackupResult {
    public data class Read(val backup: CourseBackup) : BackupResult
    public data class Unreadable(val problem: Problem) : BackupResult
}
