package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku

/**
 * The sheet a long press on a cell brings up.
 *
 * Only the things that are tedious by hand. Pencilling in every candidate a cell can take
 * is arithmetic the player can already do and would rather not do forty times, and it gives
 * away nothing the board is not showing, so it is not a hint and does not count as one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CellActionSheet(
    state: GameState,
    cell: Int,
    onAction: (GameEvent) -> Unit,
    onDismiss: () -> Unit,
) {
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
                text = "ROW $row, COLUMN $column",
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier.padding(vertical = dimens.spaceS),
            )

            if (target.isGiven) {
                Text(
                    text = "This one came with the puzzle, so it cannot be changed.",
                    style = Sendoku.type.body,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = dimens.spaceM),
                )
            } else {
                val possible = state.candidatesAt(cell)
                SheetAction(
                    label = "Pencil in all ${possible.size} candidates",
                    enabled = target.isEmpty && possible.isNotEmpty && possible != target.marks,
                ) {
                    onAction(GameEvent.FillMarks)
                }
                SheetAction("Rub out the pencil marks", enabled = target.marks.isNotEmpty) {
                    onAction(GameEvent.ClearMarks)
                }
                SheetAction("Clear the cell", enabled = !target.isEmpty || target.marks.isNotEmpty) {
                    onAction(GameEvent.Erase)
                }
            }
        }
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
