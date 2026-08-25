package com.sendoku.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sendoku.app.game.HintLevel
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A tally of what the player has asked for help with.
 *
 * Not analytics. Nothing leaves the phone, nothing is timestamped, and nothing identifies a
 * game: it is a count per technique and a count per level, and it exists for one reason,
 * which is that the technique somebody asks about most is the lesson they need next.
 *
 * Kept in preferences rather than in the database on purpose. It is a bag of counters, it
 * has no history worth keeping in order, and putting it in Room would mean a schema, a
 * migration and a test for all of that in exchange for nothing.
 */
public data class HintLog(
    val byTechnique: Map<TechniqueId, Int> = emptyMap(),
    val byLevel: Map<HintLevel, Int> = emptyMap(),
) {
    public val total: Int get() = byLevel.values.sum()

    /** The rule the player has needed most help with, if they have asked for any. */
    public val hardest: TechniqueId? get() = byTechnique.maxByOrNull { it.value }?.key
}

/** Where the tally lives. */
public interface HintLogStore {
    public val log: Flow<HintLog>
    public suspend fun record(technique: TechniqueId, level: HintLevel)
    public suspend fun clear()
}

public class DataStoreHintLog(private val store: DataStore<Preferences>) : HintLogStore {

    override val log: Flow<HintLog> = store.data.map { preferences ->
        HintLog(
            byTechnique = decode(preferences[TECHNIQUES]) { name ->
                TechniqueId.entries.firstOrNull { it.name == name }
            },
            byLevel = decode(preferences[LEVELS]) { name ->
                HintLevel.entries.firstOrNull { it.name == name }
            },
        )
    }

    override suspend fun record(technique: TechniqueId, level: HintLevel) {
        store.edit { preferences ->
            preferences[TECHNIQUES] = bump(preferences[TECHNIQUES], technique.name)
            preferences[LEVELS] = bump(preferences[LEVELS], level.name)
        }
    }

    override suspend fun clear() {
        store.edit { preferences ->
            preferences.remove(TECHNIQUES)
            preferences.remove(LEVELS)
        }
    }

    private companion object {
        val TECHNIQUES = stringPreferencesKey("hints_by_technique")
        val LEVELS = stringPreferencesKey("hints_by_level")

        /**
         * `NAME=3,CELLS=1`, and nothing cleverer.
         *
         * A json library for two dozen integers would be a dependency to justify at review
         * time. Anything unparseable is dropped rather than throwing: a counter is not worth
         * a crash, and a name this build does not know is a technique from a later version.
         */
        fun <T> decode(raw: String?, lookup: (String) -> T?): Map<T, Int> {
            if (raw.isNullOrBlank()) return emptyMap()
            val counts = HashMap<T, Int>()
            for (entry in raw.split(',')) {
                val name = entry.substringBefore('=')
                val count = entry.substringAfter('=', "").toIntOrNull() ?: continue
                val key = lookup(name) ?: continue
                counts[key] = count
            }
            return counts
        }

        fun bump(raw: String?, name: String): String {
            val counts = LinkedHashMap<String, Int>()
            for (entry in raw.orEmpty().split(',')) {
                val key = entry.substringBefore('=')
                val count = entry.substringAfter('=', "").toIntOrNull() ?: continue
                counts[key] = count
            }
            counts[name] = (counts[name] ?: 0) + 1
            return counts.entries.joinToString(",") { "${it.key}=${it.value}" }
        }
    }
}
