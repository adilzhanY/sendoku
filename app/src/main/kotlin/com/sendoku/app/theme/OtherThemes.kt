package com.sendoku.app.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Ink and Paper: the newspaper puzzle book.
 *
 * Warm paper, printed clues in black, your own digits in ink blue, and red kept back for a
 * mistake. It is the theme that reads oldest and the easiest on the eyes for a long sitting,
 * which is not a small thing for a grade that takes forty minutes.
 *
 * The blue is not decoration. The first version wrote entries in the same red family as the
 * mistakes, which under red green colour blindness made a wrong digit look exactly like a
 * right one. Writing in blue and correcting in red is what a puzzle book does anyway.
 */
public val InkLight: SendokuColors = SendokuColors(
    background = Color(0xFFF4EDE0),
    surface = Color(0xFFFAF5EA),
    surfaceRaised = Color(0xFFEFE6D5),
    hairline = Color(0x4D2B2620),
    boxLine = Color(0xFF2B2620),
    given = Color(0xFF2B2620),
    entry = Color(0xFF1B3260),
    pencil = Color(0xD0544C41),
    muted = Color(0xFF5F5747),
    accent = Color(0xFF1B3260),
    onAccent = Color(0xFFF4EDE0),
    selection = Color(0x2E1B3260),
    peer = Color(0x1F2B2620),
    match = Color(0x1F1B3260),
    conflict = Color(0xFFA1160F),
    conflictWash = Color(0x24A1160F),
    hintLogic = Color(0x3D4A5A47),
    hintStrike = Color(0x1A4A5A47),
    isDark = false,
)

/** Ink after dark. Paper does not exist at night, so this is ink on a warm near black. */
public val InkDark: SendokuColors = SendokuColors(
    background = Color(0xFF14110D),
    surface = Color(0xFF1D1913),
    surfaceRaised = Color(0xFF272118),
    hairline = Color(0x40D8CBB2),
    boxLine = Color(0xFF6B5F49),
    given = Color(0xFFEFE6D5),
    entry = Color(0xFF9CC0F5),
    pencil = Color(0xB3C4B69A),
    muted = Color(0xFFA1957C),
    accent = Color(0xFF9CC0F5),
    onAccent = Color(0xFF0A1830),
    selection = Color(0x339CC0F5),
    peer = Color(0x24D8CBB2),
    match = Color(0x249CC0F5),
    conflict = Color(0xFFFF7A6B),
    conflictWash = Color(0x26FF7A6B),
    hintLogic = Color(0x30A9BF8F),
    hintStrike = Color(0x1AA9BF8F),
    isDark = true,
)

/**
 * Slate Zen: sage and stone, and as little else as possible.
 *
 * The quiet one. Amber is kept back for the hint system alone, so that being taught
 * something always arrives in a different voice from everything else.
 */
public val ZenLight: SendokuColors = SendokuColors(
    background = Color(0xFFF2F1EC),
    surface = Color(0xFFFAFAF7),
    surfaceRaised = Color(0xFFE6E5DE),
    hairline = Color(0x3D43483F),
    boxLine = Color(0xFF43483F),
    given = Color(0xFF2E322B),
    entry = Color(0xFF44603A),
    pencil = Color(0xD44A5244),
    muted = Color(0xFF5A6151),
    accent = Color(0xFF44603A),
    onAccent = Color(0xFFF2F1EC),
    selection = Color(0x2E44603A),
    peer = Color(0x1F43483F),
    match = Color(0x1F44603A),
    conflict = Color(0xFF8F3E14),
    conflictWash = Color(0x218F3E14),
    hintLogic = Color(0x3D8A5A20),
    hintStrike = Color(0x1A8A5A20),
    isDark = false,
)

/** Zen at night. Still quiet, still sage, just turned down. */
public val ZenDark: SendokuColors = SendokuColors(
    background = Color(0xFF141613),
    surface = Color(0xFF1C1F1A),
    surfaceRaised = Color(0xFF262A23),
    hairline = Color(0x40C3CCBB),
    boxLine = Color(0xFF5C6455),
    given = Color(0xFFE7EADF),
    entry = Color(0xFF9FC98C),
    pencil = Color(0xB3ADB89F),
    muted = Color(0xFF97A18A),
    accent = Color(0xFF9FC98C),
    onAccent = Color(0xFF14200E),
    selection = Color(0x339FC98C),
    peer = Color(0x24C3CCBB),
    match = Color(0x249FC98C),
    conflict = Color(0xFFE0916A),
    conflictWash = Color(0x26E0916A),
    hintLogic = Color(0x42D8A85C),
    hintStrike = Color(0x1AD8A85C),
    isDark = true,
)

/**
 * Terminal: monospace, hard edges, and the solver worn on the outside.
 *
 * Dark only, on purpose. A terminal in light mode is a text editor, and the whole appeal of
 * this one is that it does not look like a phone app. The light variant is the dark one, and
 * that is a decision rather than an omission.
 */
public val TerminalDark: SendokuColors = SendokuColors(
    background = Color(0xFF101010),
    surface = Color(0xFF161616),
    surfaceRaised = Color(0xFF1E1E1E),
    hairline = Color(0x33E6E6E6),
    boxLine = Color(0xFF6E6E6E),
    // Dimmer than white on purpose: the lime is the player's own digits, and on a terminal
    // the two have to be separable without colour as well as with it.
    given = Color(0xFFC2C2C2),
    entry = Color(0xFFC8FF3D),
    pencil = Color(0xB0B9B9B9),
    muted = Color(0xFF9A9A9A),
    accent = Color(0xFFC8FF3D),
    onAccent = Color(0xFF101010),
    selection = Color(0x33C8FF3D),
    peer = Color(0x24CFCFCF),
    match = Color(0x24C8FF3D),
    conflict = Color(0xFFFF6A5C),
    conflictWash = Color(0x26FF6A5C),
    hintLogic = Color(0x45C8FF3D),
    hintStrike = Color(0x1AC8FF3D),
    isDark = true,
)

/** Terminal draws its own type: monospace throughout, digits included. */
public val TerminalType: SendokuType = DefaultType.copy(
    display = DefaultType.display.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.em),
    title = DefaultType.title.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.em),
    body = DefaultType.body.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
    label = DefaultType.label.copy(fontFamily = FontFamily.Monospace),
    overline = DefaultType.overline.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.1.em),
    // The grid keeps the bundled digits and keeps the weight difference. A monospace zero
    // with a slash through it is a liability on a sudoku board, and the platform monospace is
    // whatever the phone happens to have. The clue staying heavier than the entry is the only
    // thing telling them apart for a player who cannot use the colour.
)

/** Terminal has no rounding anywhere, and heavier rules. */
public val TerminalDimens: SendokuDimens = DefaultDimens.copy(
    radiusS = 0.dp,
    radiusM = 0.dp,
    radiusL = 0.dp,
    cellRadius = 0.dp,
    boardRadius = 0.dp,
    hintOutline = 3.dp,
    gridHairline = 1.dp,
    gridBoxLine = 1.dp,
    gridBorder = 1.dp,
    padGap = 3.dp,
)

/** Ink draws heavier rules than Deep Field, the way print does. */
public val InkDimens: SendokuDimens = DefaultDimens.copy(
    gridHairline = 1.dp,
    gridBoxLine = 2.dp,
    gridBorder = 3.dp,
    boardRadius = 0.dp,
    hintOutline = 3.dp,
    cellRadius = 0.dp,
    radiusS = 2.dp,
    radiusM = 3.dp,
    radiusL = 4.dp,
)

/** Zen rounds everything and keeps the rules faint. */
public val ZenDimens: SendokuDimens = DefaultDimens.copy(
    gridHairline = 1.dp,
    gridBoxLine = 1.dp,
    gridBorder = 1.dp,
    boardRadius = 6.dp,
    hintOutline = 3.dp,
    radiusS = 12.dp,
    radiusM = 18.dp,
    radiusL = 26.dp,
    padGap = 8.dp,
)

/** Terminal does not animate. Things are either one way or the other. */
public val TerminalMotion: SendokuMotion = SendokuMotion(
    instant = 0,
    quick = 0,
    brief = 0,
    settle = 60,
    celebrate = 120,
    easing = CubicBezierEasing(0f, 0f, 1f, 1f),
    enter = CubicBezierEasing(0f, 0f, 1f, 1f),
)

/** Zen moves slowly, because hurrying is the one thing it is not for. */
public val ZenMotion: SendokuMotion = DefaultMotion.copy(
    instant = 140,
    quick = 240,
    brief = 220,
    settle = 400,
    celebrate = 800,
)

/** Ink uses a serif for its headings, the way a puzzle book does. */
public val InkType: SendokuType = DefaultType.copy(
    display = DefaultType.display.copy(fontFamily = FontFamily.Serif),
    title = DefaultType.title.copy(fontFamily = FontFamily.Serif),
    body = DefaultType.body.copy(fontFamily = FontFamily.Serif, fontSize = 16.sp),
)
