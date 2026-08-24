package com.sendoku.app.ui

import androidx.annotation.StringRes
import com.sendoku.app.R
import com.sendoku.engine.Grade

/**
 * The grades, in words rather than in enum names.
 *
 * The engine carries an English display name for its own logs and tests. The app must not
 * use it: the ladder is the most visible text in the whole product, and it has to be
 * translatable like everything else.
 */
@StringRes
public fun gradeName(grade: Grade): Int = when (grade) {
    Grade.GENTLE -> R.string.grade_gentle
    Grade.STEADY -> R.string.grade_steady
    Grade.TRICKY -> R.string.grade_tricky
    Grade.SEVERE -> R.string.grade_severe
    Grade.DIABOLICAL -> R.string.grade_diabolical
    Grade.BEYOND -> R.string.grade_beyond
    Grade.INSANE -> R.string.grade_insane
    Grade.NIGHTMARE -> R.string.grade_nightmare
}

/** What a grade will ask of you, in words rather than in technique names. */
@StringRes
public fun gradeGate(grade: Grade): Int = when (grade) {
    Grade.GENTLE -> R.string.gate_gentle
    Grade.STEADY -> R.string.gate_steady
    Grade.TRICKY -> R.string.gate_tricky
    Grade.SEVERE -> R.string.gate_severe
    Grade.DIABOLICAL -> R.string.gate_diabolical
    Grade.BEYOND -> R.string.gate_beyond
    Grade.INSANE -> R.string.gate_insane
    Grade.NIGHTMARE -> R.string.gate_nightmare
}

/** The themes, in words rather than in enum names. */
@StringRes
public fun themeName(theme: com.sendoku.app.theme.SendokuThemeId): Int = when (theme) {
    com.sendoku.app.theme.SendokuThemeId.DEEP_FIELD -> R.string.theme_deep_field
    com.sendoku.app.theme.SendokuThemeId.INK -> R.string.theme_ink
    com.sendoku.app.theme.SendokuThemeId.ZEN -> R.string.theme_zen
    com.sendoku.app.theme.SendokuThemeId.TERMINAL -> R.string.theme_terminal
}

@StringRes
public fun themeSummary(theme: com.sendoku.app.theme.SendokuThemeId): Int = when (theme) {
    com.sendoku.app.theme.SendokuThemeId.DEEP_FIELD -> R.string.theme_deep_field_summary
    com.sendoku.app.theme.SendokuThemeId.INK -> R.string.theme_ink_summary
    com.sendoku.app.theme.SendokuThemeId.ZEN -> R.string.theme_zen_summary
    com.sendoku.app.theme.SendokuThemeId.TERMINAL -> R.string.theme_terminal_summary
}

@StringRes
public fun modeName(mode: com.sendoku.app.data.ThemeMode): Int = when (mode) {
    com.sendoku.app.data.ThemeMode.SYSTEM -> R.string.mode_system
    com.sendoku.app.data.ThemeMode.LIGHT -> R.string.mode_light
    com.sendoku.app.data.ThemeMode.DARK -> R.string.mode_dark
}
