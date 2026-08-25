package com.sendoku.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every measurement the app is allowed to use.
 *
 * The point is not tidiness. Three more themes are planned and one of them, Terminal, has
 * no rounding at all while another, Ink, draws heavier rules than Deep Field does. If a
 * composable writes `4.dp` the theme cannot change it, and each new look becomes a hunt
 * through the layout code instead of a new set of numbers.
 */
@Immutable
public data class SendokuDimens(
    val spaceXs: Dp,
    val spaceS: Dp,
    val spaceM: Dp,
    val spaceL: Dp,
    val spaceXl: Dp,

    val radiusS: Dp,
    val radiusM: Dp,
    val radiusL: Dp,

    /** The line between two cells inside a box. */
    val gridHairline: Dp,

    /** The line between two boxes. Heavier, because it is what makes a box a box. */
    val gridBoxLine: Dp,

    /** The line around the whole board. */
    val gridBorder: Dp,

    /** Rounding on a single cell's highlight. Zero on Deep Field, where cells are square. */
    val cellRadius: Dp,

    /** Rounding on the board as a whole. */
    val boardRadius: Dp,

    /**
     * How heavy the line around a house a hint is talking about is.
     *
     * Heavier than the box rules, and it has to be. It is drawn over a board that is already
     * a grid of lines, and a line the same weight as the furniture is furniture.
     */
    val hintOutline: Dp,

    /** Nothing tappable is ever smaller than this, however light it looks. */
    val minTouchTarget: Dp,

    /** Gap between the keys of the number pad. */
    val padGap: Dp,

    /**
     * How wide the content is allowed to get on a tall screen.
     *
     * A tablet held upright has room for a board twice the size of a phone's, and a number
     * pad stretched across all of it. Both are worse to use than the phone layout, so the
     * spare width becomes margin instead.
     */
    val contentMaxWidth: Dp,

    /** The same, for a screen wider than it is tall, where the layout puts the pad beside the board. */
    val contentMaxWidthWide: Dp,
)

/** Deep Field: square cells, hairline rules, and one heavier line to mark the boxes. */
public val DefaultDimens: SendokuDimens = SendokuDimens(
    spaceXs = 4.dp,
    spaceS = 8.dp,
    spaceM = 16.dp,
    spaceL = 24.dp,
    spaceXl = 32.dp,
    radiusS = 8.dp,
    radiusM = 12.dp,
    radiusL = 18.dp,
    gridHairline = 1.dp,
    gridBoxLine = 2.dp,
    gridBorder = 2.dp,
    cellRadius = 0.dp,
    boardRadius = 2.dp,
    hintOutline = 3.dp,
    minTouchTarget = 48.dp,
    padGap = 6.dp,
    contentMaxWidth = 600.dp,
    contentMaxWidthWide = 1000.dp,
)
