package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons
import com.sendoku.engine.Grade
import kotlin.time.Duration

/** What the home screen needs to know. */
public data class HomeState(val solvedByGrade: Map<Grade, Int>, val inProgress: InProgressSummary?)

/** The puzzle waiting to be picked up, if there is one. */
public data class InProgressSummary(val grade: Grade, val placed: Int, val total: Int, val elapsed: Duration) {
    val fraction: Float get() = if (total == 0) 0f else placed.toFloat() / total
}

/**
 * The levels, easiest first.
 *
 * Plain names. The engine grades a puzzle by the hardest technique it needs, and the words
 * that came out of that (Gentle, Tricky, Diabolical) describe the reasoning rather than the
 * difficulty. Somebody opening the app for the first time cannot tell whether Severe is
 * harder than Diabolical, and a list you cannot order is not a list of levels. Easy to
 * Master says one thing and says it immediately.
 *
 * One level opens at a time, and only by winning the one before it. The order is top to
 * bottom, easiest first, so the next thing to play is the first thing read.
 */
@Composable
public fun HomeScreen(
    state: HomeState,
    onPlay: (Grade) -> Unit,
    onResume: () -> Unit,
    onDaily: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val open = state.highestOpen()

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceM)
                .padding(top = dimens.spaceM, bottom = dimens.spaceS),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.app_name), style = Sendoku.type.title, color = colors.given)
        }

        // The ladder scrolls and the buttons do not. Pinning them means the thing a player
        // opens the app to press is always under their thumb, whatever the screen height.
        Column(
            modifier = Modifier
                .testTag("home:ladder")
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            Text(
                text = stringResource(R.string.home_choose_level),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier.padding(bottom = dimens.spaceXs),
            )

            // Easiest first, so the level to play next is the first one read rather than the
            // last. Reading a ladder from the top down and finding the bottom rung at the
            // bottom of the screen is a puzzle nobody asked for.
            for (grade in Grade.entries) {
                val locked = grade.ordinal > open.ordinal
                GradeRow(
                    grade = grade,
                    solved = state.solvedByGrade[grade] ?: 0,
                    locked = locked,
                    opensAfter = if (locked) Grade.entries[grade.ordinal - 1] else null,
                    onClick = { onPlay(grade) },
                )
            }

            state.inProgress?.let { summary ->
                Box(Modifier.padding(top = dimens.spaceS)) {
                    ContinueCard(summary, onResume)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spaceM),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        ) {
            HomeButton(
                stringResource(R.string.home_daily),
                accent = false,
                onClick = onDaily,
                modifier = Modifier.weight(1f),
            )
            HomeButton(
                label = stringResource(
                    if (state.inProgress !=
                        null
                    ) {
                        R.string.home_resume
                    } else {
                        R.string.home_new_puzzle
                    },
                ),
                accent = true,
                onClick = { if (state.inProgress != null) onResume() else onPlay(open) },
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

/**
 * The hardest level the player is allowed to start.
 *
 * Everything up to and including the first level they have not yet won. A new player has
 * exactly one level open, and each win opens exactly one more. Winning a level twice opens
 * nothing further, and losing opens nothing at all, so the only way down the list is
 * through it.
 *
 * Deliberately derived from the solve counts rather than stored. A number in the database
 * saying which level is open can drift away from the record of games played, and then a
 * player either loses levels they earned or keeps ones they did not.
 */
internal fun HomeState.highestOpen(): Grade {
    val firstUnwon = Grade.entries.indexOfFirst { (solvedByGrade[it] ?: 0) == 0 }
    return if (firstUnwon < 0) Grade.entries.last() else Grade.entries[firstUnwon]
}

@Composable
private fun GradeRow(grade: Grade, solved: Int, locked: Boolean, opensAfter: Grade?, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val name = stringResource(gradeName(grade))
    // Red, and the same red a broken digit gets. These levels cannot be solved by spotting a
    // shape, and somebody who opens one expecting the usual thing will decide the puzzle is
    // broken rather than that it is hard. The word says so as well as the colour, because a
    // colour on its own is nothing to a player who cannot tell red from grey.
    val mark = if (grade.isAdvanced) colors.conflict else colors.accent
    val below = opensAfter?.let { stringResource(gradeName(it)) }
    val advanced = stringResource(R.string.grade_advanced_talkback)
    val detail = if (below != null) {
        stringResource(R.string.grade_locked, below)
    } else {
        stringResource(gradeGate(grade))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .testTag("home:grade:${grade.name}")
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (locked) colors.surface else colors.surfaceRaised)
            .clickable(enabled = !locked, onClick = onClick)
            .alpha(if (locked) 0.55f else 1f)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS)
            .semantics(mergeDescendants = true) {
                if (locked) disabled()
                contentDescription = if (grade.isAdvanced) "$name, $advanced, $detail" else "$name, $detail"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(26.dp)
                .clip(CircleShape)
                .background(if (locked) colors.hairline else mark),
        )
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
            ) {
                Text(name, style = Sendoku.type.label, color = colors.given)
                if (grade.isAdvanced) {
                    Text(
                        text = stringResource(R.string.grade_advanced),
                        style = Sendoku.type.overline,
                        color = colors.conflict,
                    )
                }
            }
            Text(detail, style = Sendoku.type.body, color = colors.muted)
        }
        if (locked) {
            Icon(
                imageVector = SendokuIcons.Locked,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = if (solved == 0) "" else solved.toString(),
                style = Sendoku.type.timer,
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun ContinueCard(summary: InProgressSummary, onResume: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .clickable(onClick = onResume)
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.home_in_progress), style = Sendoku.type.overline, color = colors.accent)
            Text(summary.elapsed.clock(), style = Sendoku.type.timer, color = colors.muted)
        }
        Text(
            text = stringResource(
                R.string.home_percent,
                stringResource(gradeName(summary.grade)),
                (
                    summary.fraction *
                        100
                    ).toInt(),
            ),
            style = Sendoku.type.title,
            color = colors.given,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.hairline),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(summary.fraction)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
        Text(
            text = stringResource(R.string.home_placed, summary.placed, summary.total),
            style = Sendoku.type.body,
            color = colors.muted,
        )
    }
}

@Composable
private fun HomeButton(label: String, accent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Box(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Sendoku.type.label,
            color = if (accent) colors.onAccent else colors.muted,
        )
    }
}
