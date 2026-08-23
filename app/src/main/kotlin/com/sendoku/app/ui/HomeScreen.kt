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
import androidx.compose.ui.unit.dp
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import kotlin.time.Duration

/** What the home screen needs to know. */
public data class HomeState(
    val solvedByGrade: Map<Grade, Int>,
    val inProgress: InProgressSummary?,
)

/** The puzzle waiting to be picked up, if there is one. */
public data class InProgressSummary(
    val grade: Grade,
    val placed: Int,
    val total: Int,
    val elapsed: Duration,
) {
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
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val reached = state.reachedGrade()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceS),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sendoku", style = Sendoku.type.title, color = colors.given)
            Text(
                text = "SETTINGS",
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onSettings)
                    .padding(dimens.spaceS),
            )
        }

        // Hardest first, so the eye starts at the ceiling and travels down to where it is.
        for (grade in Grade.entries.reversed()) {
            GradeRow(
                grade = grade,
                solved = state.solvedByGrade[grade] ?: 0,
                aheadOfYou = grade.ordinal > reached.ordinal,
                onClick = { onPlay(grade) },
            )
        }

        Text(
            text = "THE CLIMB",
            style = Sendoku.type.overline,
            color = colors.muted,
            modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceS),
        )

        state.inProgress?.let { summary ->
            ContinueCard(summary, onResume)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceS),
            horizontalArrangement = Arrangement.spacedBy(dimens.padGap),
        ) {
            HomeButton("Daily", accent = false, onClick = onDaily, modifier = Modifier.weight(1f))
            HomeButton(
                label = if (state.inProgress != null) "Resume" else "New puzzle",
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
internal fun HomeState.reachedGrade(): Grade =
    Grade.entries.lastOrNull { (solvedByGrade[it] ?: 0) > 0 } ?: Grade.GENTLE

@Composable
private fun GradeRow(grade: Grade, solved: Int, aheadOfYou: Boolean, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
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
            Text(grade.displayName, style = Sendoku.type.label, color = colors.given)
            Text(grade.gate(), style = Sendoku.type.body, color = colors.muted)
        }
        Text(
            text = if (solved == 0) "" else solved.toString(),
            style = Sendoku.type.timer,
            color = colors.accent,
        )
    }
}

/**
 * The hardest thing a grade will ask of you, in the words the hints use.
 *
 * Taken from the rating bands rather than written by hand, so it cannot drift away from
 * what the rater actually does.
 */
internal fun Grade.gate(): String {
    val hardest = TechniqueId.entries
        .filter { com.sendoku.engine.Grade.of(it.cost) == this }
        .maxByOrNull { it.cost }
    return hardest?.let { "up to ${it.displayName.lowercase()}" } ?: "singles only"
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
            Text("IN PROGRESS", style = Sendoku.type.overline, color = colors.accent)
            Text(summary.elapsed.clock(), style = Sendoku.type.timer, color = colors.muted)
        }
        Text(
            text = "${summary.grade.displayName}, ${(summary.fraction * 100).toInt()} percent",
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
            text = "${summary.placed} of ${summary.total} placed",
            style = Sendoku.type.body,
            color = colors.muted,
        )
    }
}

@Composable
private fun HomeButton(
    label: String,
    accent: Boolean,
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
