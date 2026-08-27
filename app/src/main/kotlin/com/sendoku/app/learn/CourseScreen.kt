package com.sendoku.app.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

/**
 * The course, as a map rather than a list.
 *
 * It used to be forty two cards, one per lesson, stacked under twelve headings. That is about
 * ten screens of scrolling, it always opened at lesson one, and the only mark on the lesson
 * you were actually up to was a four pixel bar. Somebody twelve lessons in had to scroll past
 * everything they already knew, every time, to reach the next thing.
 *
 * So the page is now one pip per lesson. Twelve rows carry all forty two, filled for what has
 * been learned and ringed for where you are, and the whole course fits on one screen with the
 * shape of it visible: five lessons of first steps, then the singles, then a long climb. The
 * card for the lesson you are up to sits in the row it belongs to, because carrying on is not
 * a separate thing pinned to the top, it is the place on the map where you are standing.
 *
 * A row opens into its lessons when tapped, one at a time, so the names and the summaries are
 * still all here and the page stays short. Every lesson is tappable, finished or not and
 * reached or not: an experienced player must not have to sit through what a naked single is
 * to get to the chains, and a beginner who jumps ahead and finds it incomprehensible has
 * learned something useful about where they are.
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
    val allDone = progress.finishedCount == Curriculum.lessons.size
    // The name rather than the enum, because a saveable has to survive being written down.
    var opened by rememberSaveable { mutableStateOf<String?>(null) }

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
        }

        Column(
            modifier = Modifier
                .testTag("course:map")
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spaceM)
                .padding(bottom = dimens.spaceM),
        ) {
            Text(
                text = if (allDone) {
                    // Quiet. No confetti, no badge, no trophy case. One sentence that treats
                    // finishing as the ordinary end of a book rather than an achievement.
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
                modifier = Modifier.padding(bottom = dimens.spaceS),
            )

            if (!allDone) {
                Box(Modifier.fillMaxWidth().padding(bottom = dimens.spaceS)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(BAR)
                            .clip(CircleShape)
                            .background(colors.hairline),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.finishedCount.toFloat() / Curriculum.lessons.size)
                                .height(BAR)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                    }
                }
            }

            for (stage in Stage.entries) {
                val lessons = Curriculum.of(stage)
                if (lessons.isEmpty()) continue

                // Above the row it belongs to, so the card and the ringed pip read as one
                // thing rather than as an announcement at the top of the page.
                if (!allDone && stage == next.stage) {
                    NextCard(next, onOpen)
                }

                StageRow(
                    stage = stage,
                    lessons = lessons,
                    progress = progress,
                    next = if (allDone) null else next,
                    onClick = { opened = if (opened == stage.name) null else stage.name },
                )

                if (opened == stage.name) {
                    Column(
                        modifier = Modifier.padding(bottom = dimens.spaceS),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                    ) {
                        for (lesson in lessons) {
                            LessonRow(
                                lesson = lesson,
                                finished = progress.isFinished(lesson.id),
                                isNext = !allDone && lesson.id == next.id,
                                onClick = { onOpen(lesson.id) },
                            )
                        }
                    }
                }
            }

            // Practice used to be one word in the top corner, which is where the thing that
            // makes a technique stick had no business being.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceM)
                    .heightIn(min = dimens.minTouchTarget)
                    .clip(RoundedCornerShape(dimens.radiusM))
                    .background(colors.surface)
                    .clickable(onClick = onPractise)
                    .testTag("course:practise"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.practice_title),
                    style = Sendoku.type.label,
                    color = colors.muted,
                )
            }
        }
    }
}

/**
 * One stage, as its name and a pip for each of its lessons.
 *
 * The row is the thing you tap, not the pips. A pip is eleven density pixels across, which is
 * a quarter of the smallest thing anybody should be asked to hit, so it stays a picture and
 * the row underneath it carries the tap and the description a screen reader reads.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StageRow(
    stage: Stage,
    lessons: List<Lesson>,
    progress: CourseProgress,
    next: Lesson?,
    onClick: () -> Unit,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val name = stringResource(stage.title)
    val (done, total) = progress.of(stage)
    val here = next != null && next.stage == stage
    val counted = stringResource(R.string.course_stage_done, done, total)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .testTag("course:stage:${stage.name}")
            .clip(RoundedCornerShape(dimens.radiusS))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name, $counted"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Text(
            text = name,
            style = Sendoku.type.body,
            color = if (here) {
                colors.accent
            } else if (done == 0) {
                colors.muted
            } else {
                colors.given
            },
            modifier = Modifier.weight(1f),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PIP_GAP),
            verticalArrangement = Arrangement.spacedBy(PIP_GAP),
        ) {
            for (lesson in lessons) {
                Pip(
                    finished = progress.isFinished(lesson.id),
                    isNext = next != null && lesson.id == next.id,
                )
            }
        }
    }
}

/** One lesson, as a dot. Filled is learned, ringed is where you are, hollow is not yet. */
@Composable
private fun Pip(finished: Boolean, isNext: Boolean) {
    val colors = Sendoku.colors
    Box(
        Modifier
            .size(PIP)
            .clip(CircleShape)
            .background(if (finished) colors.accent else colors.surface)
            .border(
                width = PIP_EDGE,
                color = if (finished || isNext) colors.accent else colors.hairline,
                shape = CircleShape,
            ),
    )
}

/**
 * The lesson you are up to, said in full.
 *
 * With its own summary on it, so you know what you are about to be taught before you tap it
 * rather than after. The whole card is the button.
 */
@Composable
private fun NextCard(lesson: Lesson, onOpen: (LessonId) -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val here = stringResource(R.string.course_you_are_here)
    val title = stringResource(lesson.title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.spaceS)
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .clickable { onOpen(lesson.id) }
            .testTag("course:next")
            .padding(dimens.spaceM)
            .semantics(mergeDescendants = true) {
                contentDescription = "$here, $title"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(here, style = Sendoku.type.overline, color = colors.accent)
        Text(title, style = Sendoku.type.label, color = colors.given)
        Text(stringResource(lesson.summary), style = Sendoku.type.body, color = colors.muted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceXs)
                .heightIn(min = dimens.minTouchTarget)
                .clip(RoundedCornerShape(dimens.radiusM))
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.course_continue),
                style = Sendoku.type.label,
                color = colors.onAccent,
            )
        }
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
        Pip(finished = finished, isNext = isNext)
        Column(Modifier.weight(1f)) {
            Text(title, style = Sendoku.type.label, color = colors.given)
            Text(
                text = stringResource(lesson.summary),
                style = Sendoku.type.body,
                color = colors.muted,
                modifier = Modifier.alpha(if (finished) 0.7f else 1f),
            )
        }
    }
}

private val PIP = 11.dp
private val PIP_EDGE = 1.5.dp
private val PIP_GAP = 4.dp
private val BAR = 4.dp
