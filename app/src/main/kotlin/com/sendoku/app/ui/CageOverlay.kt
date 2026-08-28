package com.sendoku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.killer.Cage

/**
 * The cages of a Killer, drawn over the grid.
 *
 * A dashed line just inside the cells of each cage, and the sum in the corner. Both of those
 * are conventions rather than choices: every Killer anybody has ever seen in a newspaper is
 * drawn this way, and a variant that invents its own notation makes an experienced player
 * learn the app before they can play it.
 *
 * The line is inset rather than on the cell edge, so it never lands on the grid line it would
 * otherwise hide, and every cage keeps a visible gap from its neighbours. The sum goes in the
 * top left cell of the cage, which is the first cell reading order reaches, and it is drawn
 * small and dim: it is a label on the board rather than a digit in the puzzle, and a player
 * scanning for a 7 must never find one that is really a total.
 *
 * Drawn once over the whole board rather than per cell. Eighty one cells each drawing the
 * quarter of a cage they can see would be eighty one recompositions of one picture, and the
 * dashes would not line up across a cell boundary.
 */
@Composable
internal fun CageOverlay(state: GameState, modifier: Modifier = Modifier) {
    if (state.cages.isEmpty()) return

    val colors = Sendoku.colors
    val density = LocalDensity.current
    val size = state.size

    // A monospace face so the sums line up cage to cage, and small enough that a two digit
    // total still fits in the corner it is written into.
    val sumPaint = remember(colors.muted) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.muted.toArgb()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    Canvas(modifier) {
        val cell = this.size.width / size
        val inset = cell * INSET
        val dash = with(density) { DASH.toPx() }
        val gap = with(density) { GAP.toPx() }
        val stroke = with(density) { STROKE.toPx() }

        for (cage in state.cages) {
            drawCage(cage, size, cell, inset, stroke, dash, gap, colors.boxLine)
        }

        sumPaint.textSize = cell * SUM_TEXT
        drawIntoCanvas { canvas ->
            for (cage in state.cages) {
                val corner = cage.cells.min()
                val left = (corner % size) * cell + inset + stroke
                val top = (corner / size) * cell + inset + stroke
                canvas.nativeCanvas.drawText(
                    cage.sum.toString(),
                    left + cell * SUM_PAD,
                    top + sumPaint.textSize,
                    sumPaint,
                )
            }
        }
    }
}

/**
 * The dashed edge of one cage.
 *
 * Drawn side by side rather than as a path, because a cage is any connected blob and working
 * out its outline as a single path means walking the boundary in order. Four short lines per
 * cell, each one drawn only where the neighbour on that side is in a different cage, is the
 * same picture and is impossible to get wrong.
 */
private fun DrawScope.drawCage(
    cage: Cage,
    size: Int,
    cell: Float,
    inset: Float,
    stroke: Float,
    dash: Float,
    gap: Float,
    colour: androidx.compose.ui.graphics.Color,
) {
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
    val inCage = cage.cells.toSet()

    for (index in cage.cells) {
        val row = index / size
        val col = index % size
        val left = col * cell + inset
        val top = row * cell + inset
        val right = (col + 1) * cell - inset
        val bottom = (row + 1) * cell - inset

        // Above, below, left and right. A side is drawn when the cell that way is outside
        // the cage, which includes the edge of the board.
        if (row == 0 || (index - size) !in inCage) {
            line(Offset(left, top), Offset(right, top), colour, stroke, effect)
        }
        if (row == size - 1 || (index + size) !in inCage) {
            line(Offset(left, bottom), Offset(right, bottom), colour, stroke, effect)
        }
        if (col == 0 || (index - 1) !in inCage) {
            line(Offset(left, top), Offset(left, bottom), colour, stroke, effect)
        }
        if (col == size - 1 || (index + 1) !in inCage) {
            line(Offset(right, top), Offset(right, bottom), colour, stroke, effect)
        }
    }
}

private fun DrawScope.line(
    from: Offset,
    to: Offset,
    colour: androidx.compose.ui.graphics.Color,
    stroke: Float,
    effect: PathEffect,
) {
    drawLine(color = colour, start = from, end = to, strokeWidth = stroke, pathEffect = effect)
}

/** How far inside the cell the dashes sit, as a fraction of it. */
private const val INSET = 0.08f

/** The sum, as a fraction of a cell. Small: it is a label, not a digit in the puzzle. */
private const val SUM_TEXT = 0.24f

/** A hair of space between the corner and the sum, so it does not touch the dashes. */
private const val SUM_PAD = 0.03f

private val DASH: Dp = 3.dp
private val GAP: Dp = 2.5.dp
private val STROKE: Dp = 1.dp
