package com.sendoku.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sendoku.app.game.HintLevel
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The tally of what the player has asked about.
 *
 * The rule it exists to keep: a count, and nothing that could identify a game. So the tests
 * are about arithmetic and about surviving a build that does not know a name, which is what
 * happens when somebody downgrades the app or a technique is renamed.
 */
class HintLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(name: String) = DataStoreHintLog(
        PreferenceDataStoreFactory.create(produceFile = { File(folder.root, name) }),
    )

    @Test
    fun `nothing asked means nothing counted`() = runTest {
        val log = store("empty.preferences_pb").log.first()
        assertEquals(0, log.total)
        assertNull(log.hardest)
    }

    @Test
    fun `asking twice about one rule counts twice`() = runTest {
        val hints = store("counts.preferences_pb")
        hints.record(TechniqueId.X_WING, HintLevel.NAME)
        hints.record(TechniqueId.X_WING, HintLevel.FULL)
        hints.record(TechniqueId.NAKED_PAIR, HintLevel.NAME)

        val log = hints.log.first()
        assertEquals(3, log.total)
        assertEquals(2, log.byTechnique[TechniqueId.X_WING])
        assertEquals(2, log.byLevel[HintLevel.NAME])
        assertEquals(TechniqueId.X_WING, log.hardest)
    }

    @Test
    fun `clearing the record leaves nothing behind`() = runTest {
        val hints = store("cleared.preferences_pb")
        hints.record(TechniqueId.XY_WING, HintLevel.CELLS)
        hints.clear()
        assertEquals(0, hints.log.first().total)
    }
}
