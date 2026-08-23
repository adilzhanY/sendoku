package com.sendoku.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.sendoku.app.game.GameSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Where the player's preferences live. */
public interface SettingsStore {
    public val settings: Flow<GameSettings>
    public suspend fun update(transform: (GameSettings) -> GameSettings)
}

/**
 * Preferences on disk.
 *
 * The reading and writing are split out into plain functions over a [Preferences] map, so
 * the part that can actually be wrong, which key holds what and what happens when one is
 * missing, is testable without a device.
 */
public class DataStoreSettings(private val store: DataStore<Preferences>) : SettingsStore {

    override val settings: Flow<GameSettings> = store.data.map { it.toSettings() }

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        store.edit { preferences ->
            preferences.write(transform(preferences.toSettings()))
        }
    }
}

internal object SettingsKeys {
    val highlightPeers = booleanPreferencesKey("highlight_peers")
    val highlightSameDigit = booleanPreferencesKey("highlight_same_digit")
    val autoClearMarks = booleanPreferencesKey("auto_clear_marks")
    val flagConflicts = booleanPreferencesKey("flag_conflicts")
    val showTimer = booleanPreferencesKey("show_timer")
    val mistakeLimit = intPreferencesKey("mistake_limit")
}

/**
 * Reads the settings, falling back to the defaults for anything not written yet.
 *
 * A missing key has to mean the default rather than false, or a fresh install would come up
 * with every helper switched off and look broken.
 */
internal fun Preferences.toSettings(): GameSettings {
    val defaults = GameSettings()
    val limit = this[SettingsKeys.mistakeLimit]
    return GameSettings(
        highlightPeers = this[SettingsKeys.highlightPeers] ?: defaults.highlightPeers,
        highlightSameDigit = this[SettingsKeys.highlightSameDigit] ?: defaults.highlightSameDigit,
        autoClearMarks = this[SettingsKeys.autoClearMarks] ?: defaults.autoClearMarks,
        flagConflicts = this[SettingsKeys.flagConflicts] ?: defaults.flagConflicts,
        showTimer = this[SettingsKeys.showTimer] ?: defaults.showTimer,
        // Zero is how "no limit" is stored, since a preferences int cannot be null.
        mistakeLimit = limit?.takeIf { it > 0 },
    )
}

internal fun MutablePreferences.write(settings: GameSettings) {
    this[SettingsKeys.highlightPeers] = settings.highlightPeers
    this[SettingsKeys.highlightSameDigit] = settings.highlightSameDigit
    this[SettingsKeys.autoClearMarks] = settings.autoClearMarks
    this[SettingsKeys.flagConflicts] = settings.flagConflicts
    this[SettingsKeys.showTimer] = settings.showTimer
    this[SettingsKeys.mistakeLimit] = settings.mistakeLimit ?: 0
}
