package com.sendoku.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sendoku.app.game.GameSettings
import com.sendoku.app.theme.SendokuThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the app should look. Separate from [GameSettings], which is how it should play. */
public data class Appearance(
    val theme: SendokuThemeId = SendokuThemeId.DEEP_FIELD,
    val mode: ThemeMode = ThemeMode.SYSTEM,
)

/** Light, dark, or whatever the phone is doing. */
public enum class ThemeMode(public val displayName: String) {
    SYSTEM("Follow the system"),
    LIGHT("Always light"),
    DARK("Always dark"),
}

/** Where the player's preferences live. */
public interface SettingsStore {
    public val settings: Flow<GameSettings>
    public val appearance: Flow<Appearance>
    public suspend fun update(transform: (GameSettings) -> GameSettings)
    public suspend fun updateAppearance(transform: (Appearance) -> Appearance)
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

    override val appearance: Flow<Appearance> = store.data.map { it.toAppearance() }

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        store.edit { preferences ->
            preferences.write(transform(preferences.toSettings()))
        }
    }

    override suspend fun updateAppearance(transform: (Appearance) -> Appearance) {
        store.edit { preferences ->
            preferences.write(transform(preferences.toAppearance()))
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
    val hintLimit = intPreferencesKey("hint_limit")
    val haptics = booleanPreferencesKey("haptics")
    val sound = booleanPreferencesKey("sound")
    val theme = stringPreferencesKey("theme")
    val themeMode = stringPreferencesKey("theme_mode")
}

/**
 * Reads the settings, falling back to the defaults for anything not written yet.
 *
 * A missing key has to mean the default rather than false, or a fresh install would come up
 * with every helper switched off and look broken.
 */
internal fun Preferences.toSettings(): GameSettings {
    val defaults = GameSettings()
    // Absent means the default, present means what it says, and zero means switched off.
    // Reading an absent key as null would have quietly ignored the default on a fresh install,
    // which is exactly what it did the first time the limits were turned on.
    val mistakes = this[SettingsKeys.mistakeLimit]?.takeIf { it > 0 } ?: defaults.mistakeLimit
        .takeIf { SettingsKeys.mistakeLimit !in this }
    val hints = this[SettingsKeys.hintLimit]?.takeIf { it > 0 } ?: defaults.hintLimit
        .takeIf { SettingsKeys.hintLimit !in this }
    return GameSettings(
        highlightPeers = this[SettingsKeys.highlightPeers] ?: defaults.highlightPeers,
        highlightSameDigit = this[SettingsKeys.highlightSameDigit] ?: defaults.highlightSameDigit,
        autoClearMarks = this[SettingsKeys.autoClearMarks] ?: defaults.autoClearMarks,
        flagConflicts = this[SettingsKeys.flagConflicts] ?: defaults.flagConflicts,
        showTimer = this[SettingsKeys.showTimer] ?: defaults.showTimer,
        // Zero is how "no limit" is stored, since a preferences int cannot be null.
        mistakeLimit = mistakes,
        hintLimit = hints,
        haptics = this[SettingsKeys.haptics] ?: defaults.haptics,
        sound = this[SettingsKeys.sound] ?: defaults.sound,
    )
}

/**
 * Reads the look, falling back to Deep Field.
 *
 * An unknown name means a theme that used to exist and does not any more, which should look
 * like a fresh install rather than a crash.
 */
internal fun Preferences.toAppearance(): Appearance {
    val theme = this[SettingsKeys.theme]
        ?.let { name -> SendokuThemeId.entries.firstOrNull { it.name == name } }
        ?: SendokuThemeId.DEEP_FIELD
    val mode = this[SettingsKeys.themeMode]
        ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
        ?: ThemeMode.SYSTEM
    return Appearance(theme, mode)
}

internal fun MutablePreferences.write(appearance: Appearance) {
    this[SettingsKeys.theme] = appearance.theme.name
    this[SettingsKeys.themeMode] = appearance.mode.name
}

internal fun MutablePreferences.write(settings: GameSettings) {
    this[SettingsKeys.highlightPeers] = settings.highlightPeers
    this[SettingsKeys.highlightSameDigit] = settings.highlightSameDigit
    this[SettingsKeys.autoClearMarks] = settings.autoClearMarks
    this[SettingsKeys.flagConflicts] = settings.flagConflicts
    this[SettingsKeys.showTimer] = settings.showTimer
    this[SettingsKeys.mistakeLimit] = settings.mistakeLimit ?: 0
    this[SettingsKeys.hintLimit] = settings.hintLimit ?: 0
    this[SettingsKeys.haptics] = settings.haptics
    this[SettingsKeys.sound] = settings.sound
}
