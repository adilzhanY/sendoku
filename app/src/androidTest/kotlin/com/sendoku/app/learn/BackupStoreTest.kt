package com.sendoku.app.learn

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.data.FinishedRow
import com.sendoku.app.data.LessonProgressRow
import com.sendoku.app.data.SendokuDatabase
import com.sendoku.app.data.TechniqueMasteryRow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Export, wipe, import, against a real database.
 *
 * The format tests cover the file. These cover the thing that actually loses somebody's
 * record: the write and the read either side of it.
 */
@RunWith(AndroidJUnit4::class)
class BackupStoreTest {

    private lateinit var database: SendokuDatabase
    private lateinit var store: BackupStore

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SendokuDatabase::class.java,
        ).build()
        store = BackupStore(database, "test")
    }

    @After
    fun close() {
        database.close()
    }

    private suspend fun seed() {
        database.lessonProgress().save(LessonProgressRow(LessonId.NAKED_SINGLE.name, 4, true, 10))
        database.lessonProgress().save(LessonProgressRow(LessonId.X_WING.name, 2, false, 11))
        database.mastery().save(TechniqueMasteryRow("NAKED_SINGLE", 6, 5, 3, true, 12))
        database.finished().record(
            FinishedRow(
                givens = ".".repeat(81),
                grade = "SEVERE",
                rating = 6.1,
                hardest = "XY_WING",
                elapsedSeconds = 900,
                hintsUsed = 1,
                mistakes = 0,
                solved = true,
                finishedAt = 5_000,
            ),
        )
    }

    @Test
    fun aRecordSurvivesExportWipeAndImport() = runBlocking {
        seed()
        val file = CourseBackup.encode(store.export(at = 1))

        database.lessonProgress().clear()
        database.mastery().clear()
        database.finished().clear()
        assertTrue(database.lessonProgress().all().isEmpty())

        val read = CourseBackup.decode(file)
        assertTrue(read is BackupResult.Read)
        store.import((read as BackupResult.Read).backup, at = 2)

        assertEquals(2, database.lessonProgress().all().size)
        assertEquals(4, database.lessonProgress().of(LessonId.NAKED_SINGLE.name)?.step)
        assertEquals(true, database.mastery().of("NAKED_SINGLE")?.mastered)
        assertEquals(1, database.finished().all().size)
    }

    @Test
    fun importingTwiceDoesNotDoubleAnything() = runBlocking {
        seed()
        val backup = store.export(at = 1)
        store.import(backup, at = 2)
        store.import(backup, at = 3)

        assertEquals("games were counted twice", 1, database.finished().all().size)
        assertEquals(2, database.lessonProgress().all().size)
    }

    @Test
    fun importKeepsWhicheverGotFurther() = runBlocking {
        // The rule that stops a stale file eating a month of progress.
        database.lessonProgress().save(LessonProgressRow(LessonId.NAKED_SINGLE.name, 9, true, 100))
        val older = CourseBackup(lessons = listOf(LessonRecord(LessonId.NAKED_SINGLE.name, 1, false, 1)))

        store.import(older, at = 2)

        val row = database.lessonProgress().of(LessonId.NAKED_SINGLE.name)
        assertEquals("an older file overwrote newer progress", 9, row?.step)
        assertEquals("an older file un-finished a finished lesson", true, row?.finished)
    }

    @Test
    fun namesThisBuildDoesNotKnowAreCountedAndDropped() = runBlocking {
        val strange = CourseBackup(
            lessons = listOf(LessonRecord("NOT_A_LESSON", 3, true)),
            mastery = listOf(MasteryRecord("NOT_A_TECHNIQUE", 1, 1, 1, true)),
        )

        val done = store.import(strange, at = 1)

        assertEquals(2, done.ignored)
        assertEquals(0, done.lessons)
        assertTrue("an unknown name reached the database", database.lessonProgress().all().isEmpty())
    }

    @Test
    fun anEmptyFileChangesNothing() = runBlocking {
        seed()
        store.import(CourseBackup(), at = 1)
        assertEquals(2, database.lessonProgress().all().size)
        assertEquals(1, database.finished().all().size)
    }
}
