package com.sendoku.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The export format.
 *
 * Everything here is somebody's real file, so the tests are mostly about the ways a file can
 * be wrong. A round trip working is the easy half; refusing a file from a future version, and
 * surviving one that was truncated halfway through a download, is the half that matters.
 */
class CourseBackupTest {

    private fun sample() = CourseBackup(
        app = "1.0",
        writtenAt = 1_787_000_000_000,
        lessons = listOf(
            LessonRecord(LessonId.NAKED_SINGLE.name, step = 4, finished = true, at = 1),
            LessonRecord(LessonId.X_WING.name, step = 1, finished = false, at = 2),
        ),
        mastery = listOf(
            MasteryRecord("NAKED_SINGLE", attempts = 5, correct = 4, streak = 3, mastered = true, at = 3),
        ),
        games = listOf(
            GameRecord(
                givens = ".".repeat(81),
                grade = "SEVERE",
                rating = 6.1,
                hardest = "XY_WING",
                seconds = 900,
                hints = 1,
                mistakes = 0,
                solved = true,
                finishedAt = 1_787_000_000_001,
            ),
        ),
    )

    @Test
    fun `a record survives a round trip unchanged`() {
        val text = CourseBackup.encode(sample())
        val result = CourseBackup.decode(text)
        assertTrue("a file we just wrote was not readable: $result", result is BackupResult.Read)
        assertEquals(sample(), (result as BackupResult.Read).backup)
    }

    @Test
    fun `the file is readable by a person`() {
        // The app promises it collects nothing. A binary blob would undo that in one step.
        val text = CourseBackup.encode(sample())
        assertTrue("the lesson name is not in the file", text.contains("NAKED_SINGLE"))
        assertTrue("the format version is not in the file", text.contains("\"format\""))
        assertTrue("the file is not laid out for reading", text.contains("\n"))
    }

    @Test
    fun `a file from a newer app is refused rather than guessed at`() {
        val text = CourseBackup.encode(sample()).replace("\"format\": 1", "\"format\": 99")
        val result = CourseBackup.decode(text)
        assertEquals(BackupResult.Unreadable(Problem.FROM_THE_FUTURE), result)
    }

    @Test
    fun `an added field does not break an older reader`() {
        // The rule that makes adding a field a compatible change.
        val text = CourseBackup.encode(sample()).replaceFirst("{", "{\n  \"somethingNew\": 7,")
        assertTrue(CourseBackup.decode(text) is BackupResult.Read)
    }

    @Test
    fun `a truncated file is refused, not half read`() {
        val text = CourseBackup.encode(sample())
        for (cut in listOf(text.length / 4, text.length / 2, text.length - 3)) {
            val result = CourseBackup.decode(text.take(cut))
            assertTrue("a file cut at $cut was accepted", result is BackupResult.Unreadable)
        }
    }

    @Test
    fun `something that is not ours says so`() {
        assertEquals(BackupResult.Unreadable(Problem.NOT_OURS), CourseBackup.decode("hello"))
        assertEquals(BackupResult.Unreadable(Problem.NOT_OURS), CourseBackup.decode("{\"unrelated\":true"))
        assertEquals(BackupResult.Unreadable(Problem.EMPTY), CourseBackup.decode("   "))
    }

    @Test
    fun `a file naming a lesson this build has never heard of still parses`() {
        // Parsing and trusting are different things. The parse succeeds and the import drops
        // the unknown name, which is what keeps a file from a later version partly useful.
        val text = CourseBackup.encode(
            sample().copy(lessons = listOf(LessonRecord("SOME_FUTURE_LESSON", 2, true))),
        )
        val result = CourseBackup.decode(text)
        assertTrue(result is BackupResult.Read)
        assertEquals("SOME_FUTURE_LESSON", (result as BackupResult.Read).backup.lessons.single().id)
    }

    @Test
    fun `an empty record is still a valid file`() {
        val result = CourseBackup.decode(CourseBackup.encode(CourseBackup(app = "1.0")))
        assertTrue(result is BackupResult.Read)
        assertEquals(emptyList<LessonRecord>(), (result as BackupResult.Read).backup.lessons)
    }
}
