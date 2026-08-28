package com.sendoku.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The guard that makes a migration impossible to forget.
 *
 * Room writes the shape of every database version into `app/schemas`, and those files are
 * committed. Change an entity and the identity hash below stops matching, which fails here
 * rather than at runtime on a player's phone with a year of history in it.
 *
 * When that happens the fix is never to update the hash on its own. It is to bump
 * [SendokuDatabase.VERSION], write the migration, register it, and then update the hash.
 */
class SchemaGuardTest {

    private val schemaDirectory = File("schemas/com.sendoku.app.data.SendokuDatabase")

    /**
     * The shape of version 1, as shipped.
     *
     * If this test fails, read the two sentences above before touching this line.
     */
    private val knownHashes = mapOf(
        1 to "984c295e58d33ea092ed847f8e285097",
        2 to "fcaf9351619be1300598cb1a60f15054",
        3 to "9b3a2aafca5804d51ac97550ae7c1416",
        4 to "f5a2ab7cf6c0185e996d0da5b962a1f8",
        5 to "b3973db90fb7589ea82b92c0de51048b",
    )

    @Test
    fun `every version has its schema committed`() {
        assertTrue(
            "no exported schemas at ${schemaDirectory.absolutePath}, run a build first",
            schemaDirectory.isDirectory,
        )
        for (version in 1..SendokuDatabase.VERSION) {
            assertTrue("version $version has no schema file", File(schemaDirectory, "$version.json").isFile)
        }
    }

    @Test
    fun `the schema has not changed without the version changing`() {
        for ((version, expected) in knownHashes) {
            val text = File(schemaDirectory, "$version.json").readText()
            val actual = Regex("\"identityHash\"\\s*:\\s*\"([0-9a-f]+)\"").find(text)?.groupValues?.get(1)
            assertEquals(
                "version $version of the database changed shape, so it needs a new version and a migration",
                expected,
                actual,
            )
        }
    }

    @Test
    fun `the pinned versions keep up with the declared one`() {
        assertEquals(
            "SendokuDatabase.VERSION was bumped without pinning the new schema hash here",
            SendokuDatabase.VERSION,
            knownHashes.keys.max(),
        )
    }

    @Test
    fun `both tables are in the schema`() {
        val text = File(schemaDirectory, "${SendokuDatabase.VERSION}.json").readText()
        assertTrue(text.contains("\"tableName\": \"in_progress\""))
        assertTrue(text.contains("\"tableName\": \"finished\""))
    }
}
