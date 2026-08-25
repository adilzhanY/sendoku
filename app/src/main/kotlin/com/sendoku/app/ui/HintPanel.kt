package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintLevel
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku

/**
 * The hint, as much of it as has been asked for.
 *
 * There is no advertisement in front of this, no countdown, and no limit. A player who
 * needs three hints on their first Diabolical puzzle is exactly the player this app is
 * for, and charging them for it, in money or in attention, would be the whole point missed.
 */
@Composable
public fun HintPanel(
    hint: Hint,
    onMore: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onGlossary: () -> Unit,
    onRemoveMistake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        when (hint) {
            is Hint.Step -> StepBody(hint, onGlossary)

            is Hint.Mistake -> {
                Text(stringResource(R.string.hint_wrong_title), style = Sendoku.type.overline, color = colors.conflict)
                val one = hint.cells.size == 1
                Text(
                    text = if (one) {
                        stringResource(R.string.hint_wrong_one)
                    } else {
                        stringResource(R.string.hint_wrong_many, hint.cells.size)
                    },
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
                Text(
                    text = stringResource(
                        if (one) R.string.hint_wrong_until_one else R.string.hint_wrong_until_many,
                    ),
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            }

            Hint.Solved -> {
                Text(stringResource(R.string.hint_done_title), style = Sendoku.type.overline, color = colors.accent)
                Text(stringResource(R.string.hint_done_body), style = Sendoku.type.body, color = colors.muted)
            }

            Hint.Stuck -> {
                Text(stringResource(R.string.hint_stuck_title), style = Sendoku.type.overline, color = colors.muted)
                Text(
                    text = stringResource(R.string.hint_stuck_body),
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        ) {
            HintButton(
                stringResource(R.string.hint_close),
                accent = false,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tag = "hint:close",
            )
            if (hint is Hint.Mistake) {
                // Naming the broken cell and then offering nothing but Close is a dead end,
                // and the undo history is gone once the app has been closed and reopened.
                HintButton(
                    label = stringResource(
                        if (hint.cells.size == 1) R.string.hint_remove_one else R.string.hint_remove_many,
                    ),
                    accent = true,
                    onClick = onRemoveMistake,
                    modifier = Modifier.weight(1.6f),
                )
            }
            if (hint is Hint.Step) {
                if (hint.level.hasMore) {
                    HintButton(
                        label = stringResource(
                            when (hint.level) {
                                HintLevel.REGION -> R.string.hint_what_kind
                                HintLevel.NAME -> R.string.hint_where
                                else -> R.string.hint_explain
                            },
                        ),
                        accent = true,
                        onClick = onMore,
                        modifier = Modifier.weight(1.6f),
                        tag = "hint:more",
                    )
                } else {
                    HintButton(
                        stringResource(R.string.hint_do_it),
                        accent = true,
                        onClick = onApply,
                        modifier = Modifier.weight(1.6f),
                        tag = "hint:apply",
                    )
                }
            }
        }
    }
}

@Composable
private fun StepBody(hint: Hint.Step, onGlossary: () -> Unit) {
    val colors = Sendoku.colors
    val technique = hint.deduction.technique
    val region = TechniqueCopy.where(hint.deduction)

    // The quietest level names a region and stops. Not the technique, not the cells, and
    // certainly not the digit: somewhere to point your eyes, and the rest is still yours.
    if (hint.level == HintLevel.REGION) {
        Text(
            text = stringResource(R.string.hint_look_here_title),
            style = Sendoku.type.overline,
            color = colors.accent,
        )
        Text(
            text = region?.let { stringResource(R.string.hint_look_here, it) }
                ?: stringResource(R.string.hint_look_around),
            style = Sendoku.type.body,
            color = colors.given,
            modifier = Modifier.testTag("hint:region"),
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(TechniqueCopy.nameOf(technique)).uppercase(),
            style = Sendoku.type.overline,
            color = colors.accent,
        )
        if (region != null && hint.level != HintLevel.NAME) {
            Text(region, style = Sendoku.type.body, color = colors.muted)
        }
    }

    Text(
        text = stringResource(TechniqueCopy.lookFor(technique)),
        style = Sendoku.type.body,
        color = colors.given,
    )

    if (hint.level != HintLevel.NAME) {
        // Straight to the lesson that teaches this technique, not to a list of definitions.
        // A player who has just been shown an XY-Wing and does not follow it wants the one
        // that explains XY-Wings, and wants to come back to this grid afterwards.
        val teaches = Curriculum.teaching(technique) != null
        Text(
            text = stringResource(if (teaches) R.string.hint_learn_this else R.string.hint_what_is_this),
            style = Sendoku.type.overline,
            color = colors.accent,
            modifier = Modifier
                .clip(RoundedCornerShape(Sendoku.dimens.radiusS))
                .clickable(onClick = onGlossary)
                .padding(vertical = Sendoku.dimens.spaceXs)
                .testTag("hint:learn"),
        )
    }

    if (hint.restsOnEarlierHints && hint.level != HintLevel.NAME) {
        Text(
            text = stringResource(R.string.hint_rests_on_earlier),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.testTag("hint:earlier"),
        )
    }

    // The working, for the one rule where a player cannot check the claim by looking. Shown
    // from the level that names the cells onwards, since before that there is no cell to
    // check it against.
    val evidence = hint.evidence
    if (evidence != null && hint.level != HintLevel.NAME && hint.level != HintLevel.REGION) {
        Text(
            text = stringResource(
                R.string.hint_evidence,
                evidence.row.joinToString(", "),
                evidence.column.joinToString(", "),
                evidence.box.joinToString(", "),
                evidence.digit,
            ),
            style = Sendoku.type.body,
            color = colors.given,
            modifier = Modifier.testTag("hint:evidence"),
        )
    }

    if (hint.level == HintLevel.FULL) {
        Text(
            text = stringResource(TechniqueCopy.because(technique)),
            style = Sendoku.type.body,
            color = colors.muted,
        )
        Text(
            text = TechniqueCopy.outcome(hint.deduction),
            style = Sendoku.type.body,
            color = colors.accent,
        )
    }
}

@Composable
private fun HintButton(
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    HintChoice(label = label, accent = accent, tag = tag, onClick = onClick, modifier = modifier)
}

/** One button in the hint panel or the menu in front of it. */
@Composable
internal fun HintChoice(
    label: String,
    accent: Boolean,
    tag: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .then(if (tag == null) Modifier else Modifier.testTag(tag)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (accent) colors.onAccent else colors.muted,
        )
    }
}
