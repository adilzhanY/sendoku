package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.game.WhyNot
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.chainTints

/**
 * The sheet a long press on a cell brings up.
 *
 * Only the things that are tedious by hand. Noting every candidate a cell can take is
 * arithmetic the player can already do and would rather not do forty times, and it gives
 * away nothing the board is not showing, so it is not a hint and does not count as one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CellActionSheet(state: GameState, cell: Int, onAction: (GameEvent) -> Unit, onDismiss: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val target = state.cells[cell]
    val row = cell / state.size + 1
    val column = cell % state.size + 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surfaceRaised,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceM)
                .padding(bottom = dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            Text(
                text = stringResource(R.string.cell_actions_title, row, column),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier.padding(vertical = dimens.spaceS),
            )

            if (target.isGiven) {
                Text(
                    text = stringResource(R.string.cell_actions_clue),
                    style = Sendoku.type.body,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = dimens.spaceM),
                )
            } else {
                val possible = state.candidatesAt(cell)
                SheetAction(
                    label = pluralStringResource(R.plurals.cell_actions_note_all, possible.size, possible.size),
                    enabled = target.isEmpty && possible.isNotEmpty && possible != target.marks,
                ) {
                    onAction(GameEvent.FillMarks)
                }
                SheetAction(stringResource(R.string.cell_actions_clear_notes), enabled = target.marks.isNotEmpty) {
                    onAction(GameEvent.ClearMarks)
                }
                SheetAction(
                    stringResource(R.string.cell_actions_clear),
                    enabled =
                    !target.isEmpty || target.marks.isNotEmpty,
                ) {
                    onAction(GameEvent.Erase)
                }
            }

            // Colouring lives here rather than on the toolbar, because it is used in bursts
            // while following one chain and never once a game like the rest of the toolbar.
            TintRow(state, cell, onAction)

            WhyNotPanel(state, cell)
        }
    }
}

/**
 * The four tints, and a way to take them all off.
 *
 * A tint is the player's own working note, so nothing in the game reads it: it does not make
 * a cell right or wrong, it survives a digit being typed over it, and undo does not walk
 * back through it. It is a pencil, and the board is paper.
 */
@Composable
private fun TintRow(state: GameState, cell: Int, onAction: (GameEvent) -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val current = state.tints[cell]

    Text(
        text = stringResource(R.string.tint_title),
        style = Sendoku.type.overline,
        color = colors.muted,
        modifier = Modifier.padding(top = dimens.spaceM, bottom = dimens.spaceXs),
    )
    Row(
        modifier = Modifier.fillMaxWidth().testTag("tint:row"),
        horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((index, tint) in colors.chainTints.withIndex()) {
            val chosen = current == index
            val name = stringResource(tintName(index))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = dimens.minTouchTarget)
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .background(tint)
                    .border(
                        width = if (chosen) 2.dp else 1.dp,
                        color = if (chosen) colors.accent else colors.hairline,
                        shape = RoundedCornerShape(dimens.radiusS),
                    )
                    .clickable { onAction(GameEvent.Tint(cell, index)) }
                    .testTag("tint:$index")
                    .semantics {
                        contentDescription = name
                        role = Role.Button
                        this.selected = chosen
                    },
            )
        }
    }
    SheetAction(stringResource(R.string.tint_clear_all), enabled = state.tints.isNotEmpty()) {
        onAction(GameEvent.ClearTints)
    }
}

/**
 * Why each digit cannot go in this cell.
 *
 * The question a beginner asks and nothing else answers. It says nothing the board is not
 * already showing, so it costs no hint: every line is something the player could have found
 * by looking along a row, and being told where to look is how somebody learns to look.
 *
 * A digit nothing rules out is listed as exactly that. It is not a promise that the digit
 * belongs there, and the wording is careful about the difference.
 */
@Composable
private fun WhyNotPanel(state: GameState, cell: Int) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var asked by remember(cell) { mutableStateOf<Int?>(null) }
    if (!state.cells[cell].isEmpty) return

    Text(
        text = stringResource(R.string.why_not_title),
        style = Sendoku.type.overline,
        color = colors.muted,
        modifier = Modifier.padding(top = dimens.spaceM, bottom = dimens.spaceXs),
    )
    Row(
        modifier = Modifier.fillMaxWidth().testTag("whynot:digits"),
        horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
    ) {
        for (digit in 1..state.size) {
            val possible = digit in state.candidatesAt(cell)
            Text(
                text = digit.toString(),
                style = Sendoku.type.label,
                color = when {
                    asked == digit -> colors.accent
                    possible -> colors.given
                    else -> colors.muted
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = dimens.minTouchTarget)
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable { asked = digit }
                    .padding(vertical = dimens.spaceS)
                    .testTag("whynot:$digit"),
            )
        }
    }
    Text(
        text = asked?.let { answer(state, cell, it) } ?: stringResource(R.string.why_not_prompt),
        style = Sendoku.type.body,
        color = colors.given,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceXs)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("whynot:answer"),
    )
}

@Composable
private fun answer(state: GameState, cell: Int, digit: Int): String {
    val size = state.size
    return when (val reason = WhyNot.ask(state, cell, digit)) {
        is WhyNot.Reason.Taken -> stringResource(
            R.string.why_not_taken,
            digit,
            TechniqueCopy.name(reason.house),
            reason.by / size + 1,
            reason.by % size + 1,
        )

        is WhyNot.Reason.Filled -> stringResource(R.string.why_not_filled, reason.digit)

        WhyNot.Reason.Possible -> stringResource(R.string.why_not_possible, digit)
    }
}

@Composable
private fun SheetAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (enabled) colors.given else colors.muted,
        )
    }
}
