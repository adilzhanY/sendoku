package com.sendoku.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku

/**
 * The board.
 *
 * Every colour, line weight and corner radius comes from the theme, and nothing here names
 * one directly. Three more looks are planned, and one of them draws heavier rules while
 * another has no rounding at all, so a literal `1.dp` in this file would be a bug waiting
 * for the second theme.
 *
 * Cells are real composables rather than one big canvas drawing. That costs a little, and
 * it buys the ability to give each cell its own accessibility description later, which a
 * canvas could not do at all.
 */
@Composable
public fun SudokuBoard(
    state: GameState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (Int) -> Unit = {},
    /** Cells a hint's argument rests on. */
    hintLogic: Set<Int> = emptySet(),
    /** Cells a hint is about to strike a candidate from. */
    hintStrike: Set<Int> = emptySet(),
    /** Digits the player has placed that cannot be right. */
    wrong: Set<Int> = emptySet(),
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val size = state.size

    // Recomputed once per state rather than once per cell, since each is a set lookup done
    // eighty one times.
    val peers = state.highlightedPeers
    val matches = state.highlightedMatches
    val conflicts = state.conflicts

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(dimens.boardRadius))
            .background(colors.surface),
    ) {
        val cellSize: Dp = maxWidth / size
        val digitSize = cellSize.toSp(0.54f)
        val markSize = cellSize.toSp(0.20f)

        Column(Modifier.fillMaxSize()) {
            for (row in 0 until size) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until size) {
                        val index = row * size + col
                        BoardCell(
                            cell = state.cells[index],
                            isSelected = state.selected == index,
                            isPeer = index in peers,
                            isMatch = index in matches,
                            isConflict = index in conflicts || index in wrong,
                            isHintLogic = index in hintLogic,
                            isHintStrike = index in hintStrike,
                            digitSize = digitSize,
                            markSize = markSize,
                            onClick = { onSelect(index) },
                            onLongClick = { onLongPress(index) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }

        GridLines(state = state, modifier = Modifier.fillMaxSize())
    }
}

/**
 * The rules between the cells, drawn in one pass over the top.
 *
 * Bordering each cell separately would double every internal line, and the two halves
 * would not always land on the same physical pixel. One canvas keeps every line exactly one
 * hairline wide wherever it falls.
 */
@Composable
private fun GridLines(state: GameState, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val density = LocalDensity.current
    val size = state.size
    val boxWidth = state.dims.boxWidth
    val boxHeight = state.dims.boxHeight

    Canvas(modifier) {
        val step = this.size.width / size
        val hairline = with(density) { dimens.gridHairline.toPx() }
        val boxLine = with(density) { dimens.gridBoxLine.toPx() }
        val border = with(density) { dimens.gridBorder.toPx() }

        for (line in 1 until size) {
            val at = step * line
            // A column boundary falls on a box edge every boxWidth columns.
            val heavyVertical = line % boxWidth == 0
            drawLine(
                color = if (heavyVertical) colors.boxLine else colors.hairline,
                start = Offset(at, 0f),
                end = Offset(at, this.size.height),
                strokeWidth = if (heavyVertical) boxLine else hairline,
            )
            val heavyHorizontal = line % boxHeight == 0
            drawLine(
                color = if (heavyHorizontal) colors.boxLine else colors.hairline,
                start = Offset(0f, at),
                end = Offset(this.size.width, at),
                strokeWidth = if (heavyHorizontal) boxLine else hairline,
            )
        }

        drawRect(
            color = colors.boxLine,
            topLeft = Offset(border / 2, border / 2),
            size = androidx.compose.ui.geometry.Size(
                this.size.width - border,
                this.size.height - border,
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = border),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardCell(
    cell: Cell,
    isSelected: Boolean,
    isPeer: Boolean,
    isMatch: Boolean,
    isConflict: Boolean,
    isHintLogic: Boolean,
    isHintStrike: Boolean,
    digitSize: TextUnit,
    markSize: TextUnit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val motion = Sendoku.motion

    // The order matters: a conflict has to beat every other wash, and the selected cell has
    // to beat the peers it is highlighting.
    // A hint outranks everything. It is the only thing on screen the player explicitly asked
    // to be shown, and a selection wash sitting on top of it would hide the argument.
    val target = when {
        isHintLogic -> colors.hintLogic
        isHintStrike -> colors.hintStrike
        isConflict -> colors.conflictWash
        isSelected -> colors.selection
        isMatch -> colors.match
        isPeer -> colors.peer
        else -> Color.Transparent
    }
    val wash by animateColorAsState(
        targetValue = target,
        animationSpec = tween(motion.instant, easing = motion.easing),
        label = "cell wash",
    )

    val ink = when {
        isConflict -> colors.conflict
        cell.isGiven -> colors.given
        else -> colors.entry
    }

    Box(
        modifier = modifier
            .background(wash)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                // No ripple. A ripple spreading past a cell edge onto its neighbours reads
                // as though two cells were selected.
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !cell.isEmpty -> Text(
                text = cell.digit.toString(),
                style = if (cell.isGiven) Sendoku.type.gridGiven else Sendoku.type.gridEntry,
                color = ink,
                fontSize = digitSize,
            )

            cell.marks.isNotEmpty -> PencilMarks(cell, markSize)
        }
    }
}

/** The candidate digits, laid out where they will be once they are placed. */
@Composable
private fun PencilMarks(cell: Cell, markSize: TextUnit) {
    val colors = Sendoku.colors
    // Three across, always, so a mark keeps the same position as the player adds others.
    // A mark that moves when its neighbour appears is impossible to scan.
    val perRow = 3
    Column(
        modifier = Modifier.fillMaxSize().padding(1.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (column in 0 until perRow) {
                    val digit = row * perRow + column + 1
                    Text(
                        text = if (digit in cell.marks) digit.toString() else "",
                        style = Sendoku.type.pencilMark,
                        color = colors.pencil,
                        fontSize = markSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Scales a text size to the cell, so one board fits a small phone and a tablet. */
@Composable
private fun Dp.toSp(fraction: Float): TextUnit = with(LocalDensity.current) {
    (this@toSp * fraction).toSp()
}
