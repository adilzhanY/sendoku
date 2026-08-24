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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Grade
import kotlin.time.Duration

/** What the home screen needs to know. */
public data class HomeState(val solvedByGrade: Map<Grade, Int>, val inProgress: InProgressSummary?)

/** The puzzle waiting to be picked up, if there is one. */
public data class InProgressSummary(val grade: Grade, val placed: Int, val total: Int, val elapsed: Duration) {
    val fraction: Float get() = if (total == 0) 0f else placed.toFloat() / total
}

/**
 * The ascent.
 *
 * The grades run bottom to top, hardest at the top, so the ladder reads as something you
 * climb rather than a list you pick from. The ones above where the player has reached are
 * dimmed, and that is all: they are still tappable.
 *
 * Locking them was the obvious alternative and it is the wrong one. This is a free app with
 * no advertisement in it, and there is nothing to gain by telling somebody who bought a
 * sudoku app that they may not play sudoku. A grade they are not ready for will beat them,
 * which is a far better teacher than a padlock.
 */
@Composable
public fun HomeScreen(
    state: HomeState,
    onPlay: (Grade) -> Unit,
    onResume: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onLearn: () -> Unit,
    /** Lessons finished out of the whole course, for the line on the button. */
    learnProgress: Pair<Int, Int>,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val reached = state.reachedGrade()

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
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                Text(
                    text = stringResource(R.string.home_stats),
                    style = Sendoku.type.overline,
                    color = colors.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.radiusS))
                        .clickable(onClick = onStats)
                        .padding(dimens.spaceS),
                )
                Text(
                    text = stringResource(R.string.home_settings),
                    style = Sendoku.type.overline,
                    color = colors.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.radiusS))
                        .clickable(onClick = onSettings)
                        .padding(dimens.spaceS),
                )
            }
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
                text = stringResource(R.string.home_the_climb),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier.padding(bottom = dimens.spaceXs),
            )

            // Hardest first, so the eye starts at the ceiling and travels down to where it is.
            for (grade in Grade.entries.reversed()) {
                GradeRow(
                    grade = grade,
                    solved = state.solvedByGrade[grade] ?: 0,
                    aheadOfYou = grade.ordinal > reached.ordinal,
                    onClick = { onPlay(grade) },
                )
            }

            state.inProgress?.let { summary ->
                Box(Modifier.padding(top = dimens.spaceS)) {
                    ContinueCard(summary, onResume)
                }
            }

            // Under the ladder rather than above it. Somebody opening the app to play should
            // not have to scroll past a course to reach a puzzle, and somebody who wants the
            // course will find it in the one place the app puts everything else.
            LearnCard(finished = learnProgress.first, total = learnProgress.second, onClick = onLearn)
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
                onClick = { if (state.inProgress != null) onResume() else onPlay(reached) },
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

/**
 * The highest grade the player has finished something at, or the easiest if they are new.
 *
 * Used only to decide what is dimmed. It is a description of where they have got to, not a
 * permission.
 */
internal fun HomeState.reachedGrade(): Grade = Grade.entries.lastOrNull { (solvedByGrade[it] ?: 0) > 0 } ?: Grade.GENTLE

@Composable
private fun GradeRow(grade: Grade, solved: Int, aheadOfYou: Boolean, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .testTag("home:grade:${'$'}{grade.name}")
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (aheadOfYou) colors.surface else colors.surfaceRaised)
            .clickable(onClick = onClick)
            .alpha(if (aheadOfYou) 0.45f else 1f)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(26.dp)
                .clip(CircleShape)
                .background(if (aheadOfYou) colors.hairline else colors.accent),
        )
        Column(Modifier.weight(1f)) {
            Text(stringResource(gradeName(grade)), style = Sendoku.type.label, color = colors.given)
            Text(stringResource(gradeGate(grade)), style = Sendoku.type.body, color = colors.muted)
        }
        Text(
            text = if (solved == 0) "" else solved.toString(),
            style = Sendoku.type.timer,
            color = colors.accent,
        )
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

/** The way into the course, with how far through it the player is. */
@Composable
private fun LearnCard(finished: Int, total: Int, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceS)
            .heightIn(min = dimens.minTouchTarget)
            .testTag("home:learn")
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_learn), style = Sendoku.type.label, color = colors.given)
            Text(
                text = pluralStringResource(R.plurals.course_progress, total, finished, total),
                style = Sendoku.type.body,
                color = colors.muted,
            )
        }
    }
}
