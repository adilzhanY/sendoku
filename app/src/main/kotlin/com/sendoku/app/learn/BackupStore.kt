package com.sendoku.app.learn

import androidx.room.withTransaction
import com.sendoku.app.data.FinishedRow
import com.sendoku.app.data.LessonProgressRow
import com.sendoku.app.data.SendokuDatabase
import com.sendoku.app.data.TechniqueMasteryRow
import com.sendoku.app.game.PuzzleOrigin
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId

/** What an import did, so the screen can say something true rather than "done". */
public data class Imported(val lessons: Int, val mastery: Int, val games: Int, val ignored: Int)

/**
 * Reading and writing a player's record.
 *
 * Two decisions worth stating, because both are the kind that quietly ruin somebody's day.
 *
 * An import **merges** rather than replaces, keeping whichever of the two got further. A
 * replace is the version that eats a month of somebody's progress when they tap the wrong
 * button, and there is no undo for that.
 *
 * An import runs in **one transaction**, so a file that turns out to be broken halfway through
 * leaves the device exactly as it was rather than half converted.
 */
public class BackupStore(private val database: SendokuDatabase, private val appVersion: String) {

    public suspend fun export(at: Long): CourseBackup {
        val lessons = database.lessonProgress()
        val mastery = database.mastery()
        val finished = database.finished()
        return CourseBackup(
            app = appVersion,
            writtenAt = at,
            lessons = lessons.all().map { LessonRecord(it.lessonId, it.step, it.finished, it.lastSeenAt) },
            mastery = mastery.all().map {
                MasteryRecord(it.technique, it.attempts, it.correct, it.streak, it.mastered, it.lastPractisedAt)
            },
            games = finished.all().map {
                GameRecord(
                    givens = it.givens,
                    board = it.board,
                    grade = it.grade,
                    rating = it.rating,
                    hardest = it.hardest,
                    seconds = it.elapsedSeconds,
                    hints = it.hintsUsed,
                    mistakes = it.mistakes,
                    solved = it.solved,
                    finishedAt = it.finishedAt,
                    dailyEpochDay = it.dailyEpochDay,
                    origin = it.origin,
                )
            },
        )
    }

    /**
     * Puts a record back, keeping whatever is further along.
     *
     * Anything naming a lesson or a technique this build does not know is counted and dropped.
     * A file is somebody else's data and the only safe assumption is that it may say anything,
     * so nothing in it is turned into an enum without being checked first.
     */
    public suspend fun import(backup: CourseBackup, at: Long): Imported = database.withTransaction {
        val lessons = database.lessonProgress()
        val mastery = database.mastery()
        val finished = database.finished()
        var ignored = 0

        var lessonCount = 0
        for (record in backup.lessons) {
            if (runCatching { LessonId.valueOf(record.id) }.isFailure) {
                ignored++
                continue
            }
            val existing = lessons.of(record.id)
            lessons.save(
                LessonProgressRow(
                    lessonId = record.id,
                    step = maxOf(record.step, existing?.step ?: 0),
                    finished = record.finished || existing?.finished == true,
                    lastSeenAt = maxOf(record.at, existing?.lastSeenAt ?: 0),
                ),
            )
            lessonCount++
        }

        var masteryCount = 0
        for (record in backup.mastery) {
            if (runCatching { TechniqueId.valueOf(record.technique) }.isFailure) {
                ignored++
                continue
            }
            val existing = mastery.of(record.technique)
            mastery.save(
                TechniqueMasteryRow(
                    technique = record.technique,
                    attempts = maxOf(record.attempts, existing?.attempts ?: 0),
                    correct = maxOf(record.correct, existing?.correct ?: 0),
                    streak = maxOf(record.streak, existing?.streak ?: 0),
                    mastered = record.mastered || existing?.mastered == true,
                    lastPractisedAt = maxOf(record.at, existing?.lastPractisedAt ?: 0),
                ),
            )
            masteryCount++
        }

        // Games are matched on when they finished, which is unique enough in practice and does
        // not need a column of its own. Importing the same file twice must not double a total.
        val known = finished.all().map { it.finishedAt }.toSet()
        var gameCount = 0
        for (record in backup.games) {
            if (record.finishedAt in known) continue
            if (runCatching { Grade.valueOf(record.grade) }.isFailure) {
                ignored++
                continue
            }
            finished.record(
                FinishedRow(
                    givens = record.givens,
                    board = record.board,
                    grade = record.grade,
                    rating = record.rating,
                    hardest = record.hardest?.takeIf { runCatching { TechniqueId.valueOf(it) }.isSuccess },
                    elapsedSeconds = record.seconds,
                    hintsUsed = record.hints,
                    mistakes = record.mistakes,
                    solved = record.solved,
                    finishedAt = record.finishedAt,
                    dailyEpochDay = record.dailyEpochDay,
                    // Checked rather than trusted: a file is somebody else's data, and an
                    // origin this build has never heard of reads as an ordinary game.
                    origin = PuzzleOrigin.of(record.origin).name,
                ),
            )
            gameCount++
        }

        Imported(lessonCount, masteryCount, gameCount, ignored)
    }
}
