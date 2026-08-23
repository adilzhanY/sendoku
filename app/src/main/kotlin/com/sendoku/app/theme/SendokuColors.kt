package com.sendoku.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app is allowed to draw with.
 *
 * Nothing in Sendoku names a colour directly. A composable asks for `colors.given` or
 * `colors.conflict`, never for a hex value, which is what makes a second theme a data
 * change instead of a rewrite. Three more themes are planned, so this rule is load
 * bearing rather than tidy.
 *
 * The set is deliberately small and every entry has one job. There is exactly one accent,
 * and it means "the logic is here": selection, hint, progress, nothing else. A second
 * accent would make an X-Wing highlight compete with a button for attention, and the
 * highlight has to win.
 */
@Immutable
public data class SendokuColors(

    /** Behind everything. True black on the dark theme, for OLED and for long sessions. */
    val background: Color,

    /** The board itself, and anything sitting directly on the background. */
    val surface: Color,

    /** Cards and sheets that need to lift off the surface without a shadow. */
    val surfaceRaised: Color,

    /** The thin line between two cells. */
    val hairline: Color,

    /** The heavier line between two boxes. Reads as structure, not as decoration. */
    val boxLine: Color,

    /** A clue the puzzle came with. Never editable, so it is the quietest strong colour. */
    val given: Color,

    /** A digit the player put there. Wears the accent, because it is theirs. */
    val entry: Color,

    /** Pencil marks. Must be legible at a third of the size of a digit and no louder. */
    val pencil: Color,

    /** Labels, counters, timers. Present but never competing with the grid. */
    val muted: Color,

    /** The one accent. Selection, hint logic, progress. Nothing else, ever. */
    val accent: Color,

    /** Text and icons drawn on top of [accent]. */
    val onAccent: Color,

    /** The wash behind the selected cell. */
    val selection: Color,

    /** The wash behind cells sharing a row, column or box with the selection. */
    val peer: Color,

    /** The wash behind other cells holding the same digit as the selection. */
    val match: Color,

    /** A digit that breaks a rule. The only warm colour in the dark theme. */
    val conflict: Color,

    /** The wash behind a conflicting cell. */
    val conflictWash: Color,

    /** Cells a hint's argument rests on. */
    val hintLogic: Color,

    /** Candidates a hint is about to strike out. */
    val hintStrike: Color,

    /** True for a dark theme. Drives the status bar icons and nothing else. */
    val isDark: Boolean,
)

/**
 * Deep Field after dark, and the theme the app is designed around.
 *
 * True black rather than a dark grey, which costs nothing on an OLED panel and saves
 * power over a forty minute puzzle. One cyan accent carries every meaning the grid needs
 * to express, and rose appears only when something is wrong, so a red cell is impossible
 * to miss precisely because nothing else is warm.
 */
public val DeepFieldDark: SendokuColors = SendokuColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0E12),
    surfaceRaised = Color(0xFF131A21),
    hairline = Color(0x33788CA0),
    boxLine = Color(0xFF2F3B46),
    given = Color(0xFFE8F0F5),
    entry = Color(0xFF35E0D0),
    pencil = Color(0xA8A0B9CD),
    muted = Color(0xFF7D95A5),
    accent = Color(0xFF35E0D0),
    onAccent = Color(0xFF00201D),
    selection = Color(0x2935E0D0),
    peer = Color(0x1278A0BE),
    match = Color(0x1A35E0D0),
    conflict = Color(0xFFFF5C7A),
    conflictWash = Color(0x24FF5C7A),
    hintLogic = Color(0x4235E0D0),
    hintStrike = Color(0x1A35E0D0),
    isDark = true,
)

/**
 * Deep Field in daylight.
 *
 * Honest about what it is: the second theme, not the first. Roughly half of Android users
 * never leave light mode, so it has to be properly built rather than inverted, but it is
 * designed to look like the same product rather than to compete with the dark one.
 *
 * The accent is darkened well past the cyan of the dark theme. The bright cyan is lovely
 * on black and fails contrast on white, and a digit the player typed has to be readable
 * before it is pretty.
 *
 * The conflict red is darker than it needs to be for contrast alone. A mid crimson sat at
 * almost exactly the same brightness as the teal, which is invisible to anyone who cannot
 * separate the two by hue. Colour is still not the only cue for a mistake, see the shape
 * and weight cue in the accessibility work, but the colours should not make it harder.
 */
public val DeepFieldLight: SendokuColors = SendokuColors(
    background = Color(0xFFF7F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEDF2F5),
    hairline = Color(0x3D33454F),
    boxLine = Color(0xFF33454F),
    given = Color(0xFF14202A),
    entry = Color(0xFF00695E),
    pencil = Color(0xD0455A69),
    muted = Color(0xFF546A78),
    accent = Color(0xFF00695E),
    onAccent = Color(0xFFFFFFFF),
    selection = Color(0x2900695E),
    peer = Color(0x1233454F),
    match = Color(0x1A00695E),
    conflict = Color(0xFF8A0B22),
    conflictWash = Color(0x1F8A0B22),
    hintLogic = Color(0x3800695E),
    hintStrike = Color(0x1A00695E),
    isDark = false,
)
