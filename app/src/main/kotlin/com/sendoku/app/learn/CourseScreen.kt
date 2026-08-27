package com.sendoku.app.learn

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.ui.BackButton
import com.sendoku.app.ui.shout

/**
 * The course, as a list of what has been done and what is next.
 *
 * Every lesson is tappable, finished or not and reached or not. An experienced player must not
 * have to sit through what a naked single is to get to the chains, and a beginner who jumps
 * ahead and finds it incomprehensible has learned something useful about where they are. The
 * only thing the screen does about order is show it.
 */
@Composable
public fun CourseScreen(
    progress: CourseProgress,
    onOpen: (LessonId) -> Unit,
    onPractise: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val next = progress.next()

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            // Null when the course is a tab rather than a screen somebody navigated into.
            // A back link with nowhere to go is a button that does nothing when pressed.
            if (onBack != null) {
                BackButton(onClick = onBack)
            }
            Text(
                text = stringResource(R.string.course_title),
                style = Sendoku.type.title,
                color = colors.given,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.practice_title),
                style = Sendoku.type.overline,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onPractise)
                    .padding(dimens.spaceS)
                    .testTag("course:practise"),
            )
        }

        val allDone = progress.finishedCount == Curriculum.lessons.size
        Text(
            text = if (allDone) {
                // Quiet. No confetti, no badge, no trophy case. One sentence that treats
                // finishing as the ordinary end of a book rather than an achievement unlocked.
                stringResource(R.string.course_finished)
            } else {
                pluralStringResource(
                    R.plurals.course_progress,
                    Curriculum.lessons.size,
                    progress.finishedCount,
                    Curriculum.lessons.size,
                )
            },
            style = Sendoku.type.body,
            color = if (allDone) colors.accent else colors.muted,
            modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceXs),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = dimens.spaceXl),
        ) {
            for (stage in Stage.entries) {
                val lessons = Curriculum.of(stage)
                if (lessons.isEmpty()) continue
                item(key = "stage-${stage.name}") {
                    val (done, total) = progress.of(stage)
                    StageHeading(stage, done, total)
                }
                items(items = lessons, key = { it.id.name }) { lesson ->
                    LessonRow(
                        lesson = lesson,
                        finished = progress.isFinished(lesson.id),
                        isNext = lesson.id == next.id,
                        onClick = { onOpen(lesson.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StageHeading(stage: Stage, done: Int, total: Int) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceM, bottom = dimens.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(stage.title).shout(), style = Sendoku.type.overline, color = colors.muted)
        Text("$done/$total", style = Sendoku.type.overline, color = colors.muted)
    }
}

@Composable
private fun LessonRow(lesson: Lesson, finished: Boolean, isNext: Boolean, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val title = stringResource(lesson.title)
    val state = stringResource(
        when {
            finished -> R.string.course_lesson_done
            isNext -> R.string.course_lesson_next
            else -> R.string.course_lesson_todo
        },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .testTag("course:${lesson.id.name}")
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (isNext) colors.surfaceRaised else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $state"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Box(
            Modifier
                .size(MARKER)
                .clip(CircleShape)
                .background(if (finished) colors.accent else colors.hairline),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = Sendoku.type.label, color = colors.given)
            Text(
                text = stringResource(lesson.summary),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.alpha(if (finished) 0.7f else 1f),
            )
        }
        if (isNext) {
            Box(
                Modifier
                    .width(NEXT_BAR)
                    .height(MARKER)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}

private val MARKER = 10.dp
private val NEXT_BAR = 4.dp
