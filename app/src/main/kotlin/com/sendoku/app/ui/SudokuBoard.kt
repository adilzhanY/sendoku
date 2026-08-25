package com.sendoku.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.R
import com.sendoku.app.game.Cell
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.House
import com.sendoku.engine.HouseKind
import com.sendoku.engine.technique.CellDigit

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
    /** Houses the hint's argument is about, outlined on the board rather than only named. */
    hintHouses: List<House> = emptyList(),
    /** The exact pencil marks a hint is about to rule out, struck through where they are drawn. */
    struckMarks: Set<CellDigit> = emptySet(),
    /**
     * Whether everything outside the argument should step back.
     *
     * Off by default, and deliberately not something the board decides for itself. In a hint
     * or a lesson the player has asked to be shown one thing and the rest of the grid is in
     * the way. In practice they are hunting for the pattern themselves, and dimming the board
     * would answer the question they were asked.
     */
    spotlight: Boolean = false,
    /**
     * Whether the board takes taps at all.
     *
     * False while a hint is being read. A hint describes the board it was asked about, so a
     * tap that changes the board makes the explanation on screen stale, and a tap that only
     * moves the selection moves the highlight out from under the argument. Everything stays
     * drawn and everything stays readable by a screen reader; it simply stops listening.
     */
    live: Boolean = true,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val size = state.size

    // Recomputed once per state rather than once per cell, since each is a set lookup done
    // eighty one times.
    val peers = state.highlightedPeers
    val matches = state.highlightedMatches + state.highlightedHomes
    val conflicts = state.conflicts

    // What the hint is about, and therefore what may not be dimmed. A wrong digit is in here
    // whatever the hint says: the one thing more urgent than the next step is the digit that
    // makes the next step pointless.
    val lit = hintLogic + hintStrike + struckMarks.map { it.cell } + conflicts + wrong
    val geometry = com.sendoku.engine.Geometry.of(state.dims)
    val housed = hintHouses.flatMap { geometry.cellsOf(it).toList() }.toSet()
    val dimming = spotlight && (hintLogic.isNotEmpty() || hintHouses.isNotEmpty() || struckMarks.isNotEmpty())

    BoxWithConstraints(
        modifier = modifier
            .testTag("game:board")
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
                            onClick = if (live) ({ onSelect(index) }) else null,
                            onLongClick = if (live) ({ onLongPress(index) }) else null,
                            struck = struckMarks.filter { it.cell == index }.map { it.digit }.toSet(),
                            description = describe(
                                state = state,
                                index = index,
                                conflicting = index in conflicts || index in wrong,
                                role = when {
                                    index in hintLogic -> HintRole.ARGUMENT
                                    struckMarks.any { it.cell == index } -> HintRole.STRUCK
                                    index in housed -> HintRole.REGION
                                    else -> HintRole.NONE
                                },
                            ),
                            testTag = "game:cell:$index",
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }

        GridLines(state = state, modifier = Modifier.fillMaxSize())

        // One pass over the top for everything a hint draws. Eighty one cells all changing
        // their own brightness is eighty one recompositions for something that is really a
        // single picture, and the outline has to land on one pixel the way the rules do.
        HintOverlay(
            state = state,
            houses = hintHouses,
            lit = lit + housed,
            dimming = dimming,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Which part of a hint a cell belongs to, for the screen reader and for nothing else. */
internal enum class HintRole { NONE, ARGUMENT, STRUCK, REGION }

/**
 * The dim and the outline, drawn once over the finished board.
 *
 * The dim is the theme's own background laid over the cells that are not part of the
 * argument, so it reads as those cells stepping back rather than as a grey sheet over the
 * puzzle. It stops well short of hiding them: a player checking an X-Wing has to look along
 * the row at cells the hint never mentions, and a board dimmed until it is unreadable turns
 * a hint into something to obey rather than something to follow.
 */
@Composable
private fun HintOverlay(
    state: GameState,
    houses: List<House>,
    lit: Set<Int>,
    dimming: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val motion = Sendoku.motion
    val size = state.size

    // Compose scales this with the system animation setting on its own, so somebody who has
    // turned animations off gets the end state and no fade.
    val veil by animateFloatAsState(
        targetValue = if (dimming) DIM else 0f,
        animationSpec = tween(motion.brief, easing = motion.easing),
        label = "hint dim",
    )
    val edge by animateFloatAsState(
        targetValue = if (houses.isEmpty()) 0f else 1f,
        animationSpec = tween(motion.brief, easing = motion.easing),
        label = "hint outline",
    )
    if (veil <= 0.01f && edge <= 0.01f) return

    val outlineWidth = with(LocalDensity.current) { dimens.hintOutline.toPx() }
    // The theme's own corner, not one of this file's choosing. Terminal rounds nothing at
    // all, and a rounded box drawn on it is the one thing that would look imported.
    val corner = with(LocalDensity.current) { dimens.cellRadius.toPx() }

    Canvas(modifier) {
        val cell = this.size.width / size

        if (veil > 0.01f) {
            for (index in 0 until size * size) {
                if (index in lit) continue
                drawRect(
                    color = colors.background.copy(alpha = veil),
                    topLeft = Offset((index % size) * cell, (index / size) * cell),
                    size = androidx.compose.ui.geometry.Size(cell, cell),
                )
            }
        }

        if (edge > 0.01f) {
            for (house in houses) {
                val bounds = boundsOf(house, state.dims, cell)
                drawRoundRect(
                    color = colors.conflict.copy(alpha = edge),
                    topLeft = Offset(bounds.left + outlineWidth / 2, bounds.top + outlineWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        bounds.width - outlineWidth,
                        bounds.height - outlineWidth,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = outlineWidth),
                )
            }
        }
    }
}

/** How far back the rest of the board steps. Half, and no further. See [HintOverlay]. */
private const val DIM = 0.55f

/** The rectangle a house covers, in pixels, for a grid whose cells are [cell] wide. */
private fun boundsOf(house: House, dims: com.sendoku.engine.Dimensions, cell: Float): Rect {
    val size = dims.size
    return when (house.kind) {
        HouseKind.ROW -> Rect(0f, house.index * cell, size * cell, (house.index + 1) * cell)

        HouseKind.COLUMN -> Rect(house.index * cell, 0f, (house.index + 1) * cell, size * cell)

        HouseKind.BOX -> {
            val perBand = size / dims.boxWidth
            val left = (house.index % perBand) * dims.boxWidth * cell
            val top = (house.index / perBand) * dims.boxHeight * cell
            Rect(left, top, left + dims.boxWidth * cell, top + dims.boxHeight * cell)
        }
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
    struck: Set<Int>,
    digitSize: TextUnit,
    markSize: TextUnit,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val motion = Sendoku.motion
    val interaction = remember { MutableInteractionSource() }

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
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        // No ripple. A ripple spreading past a cell edge onto its neighbours
                        // reads as though two cells were selected.
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                },
            )
            // One description per cell, read as a whole, and applied after the click so it
            // lands on the node a screen reader actually focuses. Without merging, a
            // pencilled cell is announced as nine separate characters.
            .semantics(mergeDescendants = true) {
                contentDescription = description
                this.selected = isSelected
                if (onClick != null) role = Role.Button
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !cell.isEmpty -> Text(
                text = cell.digit.toString(),
                style = if (cell.isGiven) Sendoku.type.gridGiven else Sendoku.type.gridEntry,
                color = ink,
                fontSize = digitSize,
                textDecoration = decorationFor(isConflict),
            )

            cell.marks.isNotEmpty -> PencilMarks(cell, struck, markSize)
        }
    }
}

/** The candidate digits, laid out where they will be once they are placed. */
@Composable
private fun PencilMarks(cell: Cell, struck: Set<Int>, markSize: TextUnit) {
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
                    // A hint that rules the seven out of three cells crosses out three
                    // sevens. Tinting the whole cell instead says something is happening
                    // here and nothing at all about which digit is in trouble.
                    val dying = digit in struck
                    Text(
                        text = if (digit in cell.marks) digit.toString() else "",
                        style = Sendoku.type.pencilMark,
                        color = if (dying) colors.conflict else colors.pencil,
                        fontSize = markSize,
                        textAlign = TextAlign.Center,
                        textDecoration = if (dying) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The line under a digit that repeats.
 *
 * Colour is not the only cue for a mistake, and cannot be: rose and cyan land at almost the
 * same brightness for somebody with red green colour blindness, and identically in
 * greyscale. The underline is what actually carries it.
 */
internal fun decorationFor(isConflict: Boolean): TextDecoration? = if (isConflict) TextDecoration.Underline else null

/**
 * What a screen reader says about a cell.
 *
 * Coordinates first, because that is what orients somebody who cannot see the grid, then
 * what is in it. Pencil marks are read out in full: they are the working, and hiding them
 * from a screen reader would make the board unplayable rather than merely harder.
 */
@Composable
@ReadOnlyComposable
internal fun describe(state: GameState, index: Int, conflicting: Boolean, role: HintRole = HintRole.NONE): String {
    val cell = state.cells[index]
    val position = stringResource(
        R.string.cell_position,
        index / state.size + 1,
        index % state.size + 1,
    )
    val body = when {
        cell.isGiven -> stringResource(R.string.cell_clue, position, cell.digit)

        !cell.isEmpty -> stringResource(R.string.cell_digit, position, cell.digit)

        cell.marks.isNotEmpty ->
            stringResource(R.string.cell_noted, position, cell.marks.toList().joinToString(", "))

        else -> stringResource(R.string.cell_empty, position)
    }
    val said = if (conflicting) stringResource(R.string.cell_repeated, body) else body
    // Colour is not a cue for everybody, and a dimmed board is nothing at all to somebody
    // using a screen reader, so what the highlight means is said out loud.
    return when (role) {
        HintRole.ARGUMENT -> stringResource(R.string.cell_hint_argument, said)
        HintRole.STRUCK -> stringResource(R.string.cell_hint_struck, said)
        HintRole.REGION -> stringResource(R.string.cell_hint_region, said)
        HintRole.NONE -> said
    }
}

/** Scales a text size to the cell, so one board fits a small phone and a tablet. */
@Composable
private fun Dp.toSp(fraction: Float): TextUnit = with(LocalDensity.current) {
    (this@toSp * fraction).toSp()
}
