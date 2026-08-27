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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sendoku.app.R
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintLevel
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons

/**
 * The hint, one card at a time.
 *
 * It used to be a single panel that grew: ask for more and another paragraph appeared under
 * the last one, until the whole argument for an ALS-XY-Wing was on screen at once and the
 * player was reading an essay with a board above it. Nobody reads that. They skim it, miss
 * the sentence that mattered, and press the button that fills the cell in.
 *
 * So it is a deck now. One card says one thing, the dots say how far through the deck you
 * are, and the arrows walk it. Each card is also a level of the hint: where to look, what
 * kind of step it is, which cells it rests on, and only then the reasoning and the move.
 * Going forward reveals the next thing, going back costs nothing, and the board dims and
 * lights to match whichever card is showing.
 *
 * There is no advertisement in front of this, no countdown, and no limit. A player who
 * needs three hints on their first Diabolical puzzle is exactly the player this app is
 * for, and charging them for it, in money or in attention, would be the whole point missed.
 */
@Composable
public fun HintPanel(
    hint: Hint,
    onMore: () -> Unit,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onGlossary: () -> Unit,
    onRemoveMistake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val step = hint as? Hint.Step

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        // The heading sits in the middle of the card with the way out on the right of it.
        // The padding on both sides is the width of that button, so a long technique name
        // stays centred on the card rather than centred on what is left of it.
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = titleOf(hint),
                style = Sendoku.type.overline,
                color = if (hint is Hint.Mistake) colors.conflict else colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ARROW).align(Alignment.Center),
            )
            IconAction(
                icon = SendokuIcons.Close,
                description = stringResource(R.string.hint_close),
                onClick = onDismiss,
                tag = "hint:close",
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        // One card of words, in a space that does not change size. The panel takes the whole
        // of the room the controls were using, so the arrows and the dots stay in one place
        // as the deck is walked. A card with more to say than fits scrolls inside it rather
        // than pushing the arrows down the screen or off the bottom of it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BODY)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            when (hint) {
                is Hint.Step -> StepCard(hint, onGlossary)

                is Hint.Mistake -> MistakeCard(hint)

                Hint.Solved -> Text(
                    text = stringResource(R.string.hint_done_body),
                    style = Sendoku.type.body,
                    color = colors.muted,
                )

                Hint.Stuck -> Text(
                    text = stringResource(R.string.hint_stuck_body),
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            }
        }

        // The move itself, outside the part that scrolls. It is the conclusion of everything
        // above it and the thing the button next to it will do, and on a short phone it was
        // the first line to disappear under the fold.
        if (step != null && step.level == HintLevel.FULL) {
            Text(
                text = TechniqueCopy.outcome(step.deduction),
                style = Sendoku.type.body,
                color = colors.accent,
                modifier = Modifier.fillMaxWidth().testTag("hint:outcome"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceXs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step != null) {
                IconAction(
                    icon = SendokuIcons.Back,
                    description = stringResource(R.string.hint_previous),
                    onClick = onBack,
                    enabled = step.level.hasLess,
                    tag = "hint:back",
                )
                Dots(current = step.level.ordinal, count = HintLevel.entries.size)
                if (step.level.hasMore) {
                    IconAction(
                        icon = SendokuIcons.Forward,
                        description = stringResource(
                            when (step.level) {
                                HintLevel.REGION -> R.string.hint_what_kind
                                HintLevel.NAME -> R.string.hint_where
                                else -> R.string.hint_explain
                            },
                        ),
                        onClick = onMore,
                        accent = true,
                        tag = "hint:more",
                    )
                } else {
                    // The move is only ever offered at the end of the deck, next to the
                    // argument for it. That is the rule that keeps this a teaching tool.
                    HintChoice(
                        label = stringResource(R.string.hint_do_it),
                        accent = true,
                        tag = "hint:apply",
                        onClick = onApply,
                    )
                }
            } else {
                Box(Modifier)
                if (hint is Hint.Mistake) {
                    // Naming the broken cell and then offering nothing but Close is a dead
                    // end, and the undo history is gone once the app has been reopened.
                    HintChoice(
                        label = stringResource(
                            if (hint.cells.size == 1) R.string.hint_remove_one else R.string.hint_remove_many,
                        ),
                        accent = true,
                        tag = "hint:remove",
                        onClick = onRemoveMistake,
                    )
                }
            }
        }
    }
}

/** How far through the deck, as one dot per card. */
@Composable
private fun Dots(current: Int, count: Int) {
    val colors = Sendoku.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("hint:dots"),
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == current) DOT_ON else DOT_OFF)
                    .clip(CircleShape)
                    .background(if (index == current) colors.accent else colors.muted)
                    .alpha(if (index <= current) 1f else 0.35f),
            )
        }
    }
}

/** The heading on the card. The technique itself, once the deck has got as far as naming it. */
@Composable
private fun titleOf(hint: Hint): String = when {
    hint is Hint.Step && hint.level == HintLevel.REGION -> stringResource(R.string.hint_look_here_title)
    hint is Hint.Step -> stringResource(TechniqueCopy.nameOf(hint.deduction.technique))
    hint is Hint.Mistake -> stringResource(R.string.hint_wrong_title)
    hint is Hint.Solved -> stringResource(R.string.hint_done_title)
    else -> stringResource(R.string.hint_stuck_title)
}

@Composable
private fun MistakeCard(hint: Hint.Mistake) {
    val colors = Sendoku.colors
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
        text = stringResource(if (one) R.string.hint_wrong_until_one else R.string.hint_wrong_until_many),
        style = Sendoku.type.body,
        color = colors.muted,
    )
}

/**
 * One card of the hint, and only that card.
 *
 * The four of them are the four levels: where to look, what kind of step, which cells, and
 * the whole argument. Nothing repeats itself from the card before, apart from the working
 * behind a single, which is short and is the one thing a player cannot check by looking.
 */
@Composable
private fun StepCard(hint: Hint.Step, onGlossary: () -> Unit) {
    val colors = Sendoku.colors
    val technique = hint.deduction.technique
    val region = TechniqueCopy.where(hint.deduction)

    when (hint.level) {
        // The quietest card names a region and stops. Not the technique, not the cells, and
        // certainly not the digit: somewhere to point your eyes, and the rest is still yours.
        HintLevel.REGION -> Text(
            text = region?.let { stringResource(R.string.hint_look_here, it) }
                ?: stringResource(R.string.hint_look_around),
            style = Sendoku.type.body,
            color = colors.given,
            modifier = Modifier.testTag("hint:region"),
        )

        HintLevel.NAME -> Text(
            text = stringResource(TechniqueCopy.lookFor(technique)),
            style = Sendoku.type.body,
            color = colors.given,
        )

        HintLevel.CELLS -> {
            Text(
                text = region?.let { stringResource(R.string.hint_cells_here, it) }
                    ?: stringResource(R.string.hint_detail_cells_note),
                style = Sendoku.type.body,
                color = colors.given,
            )
            Evidence(hint)
        }

        HintLevel.FULL -> {
            Text(
                text = stringResource(TechniqueCopy.because(technique)),
                style = Sendoku.type.body,
                color = colors.given,
            )
            Evidence(hint)
        }
    }

    if (hint.restsOnEarlierHints && hint.level != HintLevel.NAME && hint.level != HintLevel.REGION) {
        Text(
            text = stringResource(R.string.hint_rests_on_earlier),
            style = Sendoku.type.body,
            color = colors.muted,
            modifier = Modifier.testTag("hint:earlier"),
        )
    }

    if (hint.level != HintLevel.REGION) {
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
}

/**
 * The working, for the one rule where a player cannot check the claim by looking.
 *
 * On both of the last two cards rather than only one of them. A player whose settings open
 * the deck at the last card would otherwise have to walk backwards to find the one sentence
 * that shows the claim is true.
 */
@Composable
private fun Evidence(hint: Hint.Step) {
    val evidence = hint.evidence ?: return
    Text(
        text = stringResource(
            R.string.hint_evidence,
            evidence.row.joinToString(", "),
            evidence.column.joinToString(", "),
            evidence.box.joinToString(", "),
            evidence.digit,
        ),
        style = Sendoku.type.body,
        color = Sendoku.colors.given,
        modifier = Modifier.testTag("hint:evidence"),
    )
}

/** An arrow or a cross, sized to a thumb rather than to the drawing inside it. */
@Composable
private fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    val colors = Sendoku.colors
    Box(
        modifier = modifier
            .size(ARROW)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.25f)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (accent) colors.accent else colors.muted,
        )
    }
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
            .padding(horizontal = dimens.spaceM)
            .then(if (tag == null) Modifier else Modifier.testTag(tag)),
        contentAlignment = Alignment.Center,
    ) {
        // Whole words, however long the language makes them. These labels are sentences in
        // German, and a button that says "Schlie" over "ßen" reads as a broken app.
        OneLine(label, Sendoku.type.label, if (accent) colors.onAccent else colors.muted, min = 6.sp)
    }
}

private val ARROW = 48.dp
private val BODY = 96.dp
private val DOT_ON = 8.dp
private val DOT_OFF = 6.dp
