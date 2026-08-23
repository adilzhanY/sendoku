package com.sendoku.app.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/**
 * How long things take, and how they get there.
 *
 * A sudoku app is played in short bursts of attention, and an animation that outstays its
 * welcome is worse than none. Selection has to feel like the cell was already selected, so
 * it is barely a transition at all. Only the win screen gets to take its time.
 *
 * These are durations in milliseconds rather than typed values, because that is what the
 * animation APIs want and wrapping them buys nothing.
 */
@Immutable
public data class SendokuMotion(
    /** Selection moving between cells. Fast enough to feel like a direct response. */
    val instant: Int,

    /** A digit appearing, a pencil mark toggling. */
    val quick: Int,

    /** A panel or sheet arriving. */
    val settle: Int,

    /** The win screen, and nothing else. */
    val celebrate: Int,

    /** The curve almost everything uses. */
    val easing: Easing,

    /** For something arriving on screen rather than changing in place. */
    val enter: Easing,
)

public val DefaultMotion: SendokuMotion = SendokuMotion(
    instant = 90,
    quick = 160,
    settle = 280,
    celebrate = 520,
    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
)
