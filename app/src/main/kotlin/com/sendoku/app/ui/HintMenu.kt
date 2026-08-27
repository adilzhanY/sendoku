package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintEngine
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Grade

/**
 * What the hint button opens: a choice, not an answer.
 *
 * Hints are limited, so spending one has to be a decision rather than a reflex. Two of the
 * things a stuck player wants are not hints at all and are given away here for nothing: how
 * hard the next step is, and whether anything already on the board is wrong. Neither says
 * where to look or what to do, so neither costs a thing.
 *
 * The two that do cost say so, next to how many are left. An app that quietly spends a
 * limited resource on a tap is an app that will end somebody's game by accident.
 */
@Composable
public fun HintMenu(
    state: GameState,
    checked: Int?,
    onCheck: () -> Unit,
    onLook: () -> Unit,
    onExplain: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val next = HintEngine.next(state)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .padding(dimens.spaceM)
            .testTag("hint:menu"),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Text(stringResource(R.string.hint_menu_title), style = Sendoku.type.overline, color = colors.accent)

        // The words scroll and the four buttons do not. In German at a large font scale
        // these three sentences are taller than what is left of the screen under the board,
        // and without this the choices themselves went off the bottom of it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            Text(
                text = difficultyOf(next),
                style = Sendoku.type.body,
                color = colors.given,
                modifier = Modifier.testTag("hint:difficulty"),
            )

            if (checked != null) {
                Text(
                    text = if (checked == 0) {
                        stringResource(R.string.hint_check_clean)
                    } else {
                        pluralStringResource(R.plurals.hint_check_wrong, checked, checked)
                    },
                    style = Sendoku.type.body,
                    color = if (checked == 0) colors.accent else colors.conflict,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("hint:check"),
                )
            }

            val left = state.settings.hintLimit?.minus(state.hintsUsed)
            Text(
                text = if (left == null) {
                    stringResource(R.string.hint_menu_free)
                } else {
                    pluralStringResource(R.plurals.hint_menu_left, left.coerceAtLeast(0), left.coerceAtLeast(0))
                },
                style = Sendoku.type.body,
                color = colors.muted,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        ) {
            MenuButton(
                label = stringResource(R.string.hint_menu_check),
                accent = false,
                tag = "hint:menu:check",
                onClick = onCheck,
                modifier = Modifier.weight(1f),
            )
            MenuButton(
                label = stringResource(R.string.hint_menu_look),
                accent = false,
                tag = "hint:menu:look",
                onClick = onLook,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        ) {
            MenuButton(
                label = stringResource(R.string.hint_close),
                accent = false,
                tag = "hint:menu:close",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            MenuButton(
                label = stringResource(R.string.hint_menu_explain),
                accent = true,
                tag = "hint:menu:explain",
                onClick = onExplain,
                modifier = Modifier.weight(1.6f),
            )
        }
    }
}

/**
 * How hard the next step is, without saying what or where it is.
 *
 * Named by the level the technique belongs to, which is the same scale the puzzle grades
 * use, so "this needs an Expert technique" means exactly what it means on the home screen.
 * Knowing that is often enough to decide whether to keep looking or to spend a hint, and it
 * gives away nothing about the grid.
 */
@Composable
private fun difficultyOf(hint: Hint): String = when (hint) {
    is Hint.Step -> stringResource(
        R.string.hint_menu_difficulty,
        stringResource(gradeName(Grade.of(hint.deduction.technique.cost))),
    )

    is Hint.Mistake -> stringResource(R.string.hint_menu_broken)

    Hint.Solved -> stringResource(R.string.hint_done_body)

    Hint.Stuck -> stringResource(R.string.hint_stuck_body)
}

@Composable
private fun MenuButton(
    label: String,
    accent: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HintChoice(label = label, accent = accent, tag = tag, onClick = onClick, modifier = modifier)
}
