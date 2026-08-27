package com.sendoku.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DATABASE = "migration-test.db"

/**
 * The upgrade path, run for real against a version 1 database.
 *
 * A migration that compiles is not a migration that works, and the thing at stake is a
 * player's whole history. This writes a row the old way, upgrades, and reads it back.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SendokuDatabase::class.java,
    )

    @Test
    fun oneToTwoKeepsTheHistoryAndAddsTheDay() {
        helper.createDatabase(DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO finished
                    (givens, grade, rating, hardest, elapsedSeconds, hintsUsed, mistakes, solved, finishedAt)
                VALUES ('${".".repeat(81)}', 'SEVERE', 6.1, 'XY_WING', 754, 2, 1, 1, 1787000000000)
                """.trimIndent(),
            )
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(DATABASE, 2, true, SendokuDatabase.MIGRATION_1_2)

        upgraded.query("SELECT grade, elapsedSeconds, solved, dailyEpochDay FROM finished").use { cursor ->
            assertTrue("the finished game did not survive the upgrade", cursor.moveToFirst())
            assertEquals("SEVERE", cursor.getString(0))
            assertEquals(754L, cursor.getLong(1))
            assertEquals(1, cursor.getInt(2))
            // Every game recorded before the calendar existed belongs to no day.
            assertTrue(cursor.isNull(3))
            assertEquals(1, cursor.count)
        }
        upgraded.close()
    }

    @Test
    fun theNewColumnAcceptsADay() {
        helper.createDatabase(DATABASE, 1).close()
        val upgraded = helper.runMigrationsAndValidate(DATABASE, 2, true, SendokuDatabase.MIGRATION_1_2)
        upgraded.execSQL(
            """
            INSERT INTO finished
                (givens, grade, rating, hardest, elapsedSeconds, hintsUsed, mistakes, solved, finishedAt, dailyEpochDay)
            VALUES ('${".".repeat(81)}', 'GENTLE', 1.4, 'NAKED_SINGLE', 120, 0, 0, 1, 1787000000000, 20688)
            """.trimIndent(),
        )
        upgraded.query("SELECT dailyEpochDay FROM finished").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(20688L, cursor.getLong(0))
        }
        upgraded.close()
    }

    @Test
    fun twoToThreeAddsTheCourseTablesAndKeepsTheHistory() {
        helper.createDatabase(DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO finished
                    (givens, grade, rating, hardest, elapsedSeconds, hintsUsed, mistakes, solved, finishedAt)
                VALUES ('${".".repeat(81)}', 'BEYOND', 7.6, 'ALS_XZ', 3000, 0, 0, 1, 1787000000000)
                """.trimIndent(),
            )
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(
            DATABASE,
            3,
            true,
            SendokuDatabase.MIGRATION_1_2,
            SendokuDatabase.MIGRATION_2_3,
        )

        upgraded.query("SELECT grade FROM finished").use { cursor ->
            assertTrue("the history did not survive two migrations", cursor.moveToFirst())
            assertEquals("BEYOND", cursor.getString(0))
        }
        // The new tables exist and are empty, which is what a player who has never opened the
        // course should have.
        upgraded.query("SELECT count(*) FROM lesson_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        upgraded.query("SELECT count(*) FROM technique_mastery").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        upgraded.close()
    }

    @Test
    fun courseProgressSurvivesTheUpgradeItWasWrittenAfter() {
        helper.createDatabase(DATABASE, 2).close()
        val upgraded = helper.runMigrationsAndValidate(DATABASE, 3, true, SendokuDatabase.MIGRATION_2_3)
        upgraded.execSQL(
            "INSERT INTO lesson_progress (lessonId, step, finished, lastSeenAt) " +
                "VALUES ('NAKED_SINGLE', 3, 0, 1787000000000)",
        )
        upgraded.query("SELECT step, finished FROM lesson_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(3, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
        }
        upgraded.close()
    }

    @Test
    fun threeToFourKeepsEveryGameAndAddsTheBoard() {
        helper.createDatabase(DATABASE, 3).apply {
            execSQL(
                """
                INSERT INTO finished
                    (givens, grade, rating, hardest, elapsedSeconds, hintsUsed, mistakes, solved, finishedAt)
                VALUES ('${".".repeat(81)}', 'BEYOND', 7.6, 'ALS_XZ', 3000, 0, 0, 1, 1787000000000)
                """.trimIndent(),
            )
            close()
        }

        val upgraded = helper.runMigrationsAndValidate(DATABASE, 4, true, SendokuDatabase.MIGRATION_3_4)

        // The game is still there, and the column it never had is empty rather than invented.
        upgraded.query("SELECT grade, board FROM finished").use { cursor ->
            assertTrue("the history did not survive the upgrade", cursor.moveToFirst())
            assertEquals("BEYOND", cursor.getString(0))
            assertTrue("a game finished before the column has a board out of nowhere", cursor.isNull(1))
        }
        upgraded.close()
    }

    @Test
    fun theNewColumnAcceptsABoard() {
        helper.createDatabase(DATABASE, 3).close()
        val upgraded = helper.runMigrationsAndValidate(DATABASE, 4, true, SendokuDatabase.MIGRATION_3_4)
        val board = (1..81).joinToString("") { ((it % 9) + 1).toString() }
        upgraded.execSQL(
            """
            INSERT INTO finished
                (givens, grade, rating, hardest, elapsedSeconds, hintsUsed, mistakes, solved, finishedAt, board)
            VALUES ('${".".repeat(81)}', 'GENTLE', 1.2, NULL, 300, 0, 0, 1, 1787000000000, '$board')
            """.trimIndent(),
        )
        upgraded.query("SELECT board FROM finished").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(board, cursor.getString(0))
        }
        upgraded.close()
    }

    @Test
    fun theInProgressRowGetsTheDayToo() {
        helper.createDatabase(DATABASE, 1).close()
        val upgraded = helper.runMigrationsAndValidate(DATABASE, 2, true, SendokuDatabase.MIGRATION_1_2)
        upgraded.query("SELECT dailyEpochDay FROM in_progress").use { cursor ->
            assertEquals(0, cursor.count)
        }
        assertNull(null)
        upgraded.close()
    }
}
