package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.sendoku.app.R
import com.sendoku.app.data.DailyDays
import com.sendoku.app.theme.Sendoku
import com.sendoku.engine.catalog.DailyPuzzle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * The daily puzzle, and the month it sits in.
 *
 * A calendar rather than a single button, because the daily is only interesting if missing
 * one costs something and catching up is possible. Every past day is playable: the puzzle
 * for a date is derived from the date itself, so yesterday's is still there and always will
 * be, with no server to ask.
 *
 * Days after today are drawn but not tappable. Not because the puzzle is unknown, it is
 * perfectly computable, but because handing somebody next Thursday today empties the point
 * of a daily.
 */
@Composable
public fun DailyScreen(
    today: LocalDate,
    days: DailyDays,
    onPlay: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val shown = remember(month) { YearMonth.parse(month) }
    val streak = remember(days, today) { dailyStreak(days.solved, today) }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            BackButton(onClick = onBack)
            Text(stringResource(R.string.daily_title), style = Sendoku.type.title, color = colors.given)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            Text(
                text = if (streak == 0) {
                    stringResource(R.string.daily_no_streak)
                } else {
                    pluralStringResource(R.plurals.daily_streak, streak, streak)
                },
                style = Sendoku.type.body,
                color = if (streak == 0) colors.muted else colors.accent,
            )

            MonthHeader(
                month = shown,
                onPrevious = { month = shown.minusMonths(1).toString() },
                onNext = { month = shown.plusMonths(1).toString() },
                canGoNext = shown < YearMonth.from(today),
            )

            WeekdayRow()
            MonthGrid(shown, today, days, onPlay)

            // The calendar is the point of the screen, but the common case is still "give me
            // today's", and hunting for the right square to do that is silly.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceS)
                    .heightIn(min = dimens.minTouchTarget)
                    .clip(RoundedCornerShape(dimens.radiusM))
                    .background(colors.accent)
                    .clickable { onPlay(today.toEpochDay()) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.daily_play_today),
                    style = Sendoku.type.label,
                    color = colors.onAccent,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, canGoNext: Boolean) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    // Read through the configuration, so switching language redraws the month name instead of
    // leaving January in English on a Russian phone until the screen is rebuilt.
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Arrow(stringResource(R.string.daily_previous_month), enabled = true, onClick = onPrevious)
        Text(
            text = "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
            style = Sendoku.type.label,
            color = colors.given,
        )
        Arrow(stringResource(R.string.daily_next_month), enabled = canGoNext, onClick = onNext)
    }
    Box(Modifier.padding(top = dimens.spaceXs))
}

@Composable
private fun Arrow(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Text(
        text = label,
        style = Sendoku.type.overline,
        color = colors.muted,
        modifier = Modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusS))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.3f)
            .padding(dimens.spaceS),
    )
}

@Composable
private fun WeekdayRow() {
    val colors = Sendoku.colors
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth()) {
        // Monday first, to match the grade rotation, which treats Monday as the start.
        for (offset in 0..6) {
            val day = java.time.DayOfWeek.MONDAY.plus(offset.toLong())
            Text(
                text = day.getDisplayName(TextStyle.NARROW, locale),
                style = Sendoku.type.overline,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, days: DailyDays, onPlay: (Long) -> Unit) {
    val dimens = Sendoku.dimens
    val first = month.atDay(1)
    // Monday is one in java.time, so this is how many blanks come before the first.
    val leading = first.dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                for (column in 0..6) {
                    val index = row * 7 + column
                    val dayOfMonth = index - leading + 1
                    if (dayOfMonth < 1 || dayOfMonth > month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            mark = markFor(date, today, days),
                            isToday = date == today,
                            modifier = Modifier.weight(1f),
                            onClick = { onPlay(date.toEpochDay()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, mark: DayMark, isToday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val background = when (mark) {
        DayMark.SOLVED -> colors.accent
        DayMark.ATTEMPTED -> colors.surfaceRaised
        DayMark.UNPLAYED -> colors.surface
        DayMark.FUTURE -> colors.surface
    }
    val ink = when (mark) {
        DayMark.SOLVED -> colors.onAccent
        DayMark.FUTURE -> colors.muted
        else -> colors.given
    }
    val state = stringResource(
        when (mark) {
            DayMark.SOLVED -> R.string.daily_cell_solved
            DayMark.ATTEMPTED -> R.string.daily_cell_attempted
            DayMark.UNPLAYED -> R.string.daily_cell_unplayed
            DayMark.FUTURE -> R.string.daily_cell_future
        },
    )
    val grade = stringResource(gradeName(DailyPuzzle.gradeFor(date.toEpochDay())))
    val description = if (isToday) {
        stringResource(R.string.daily_cell_today, date.dayOfMonth, grade, state)
    } else {
        stringResource(R.string.daily_cell, date.dayOfMonth, grade, state)
    }

    val shape = RoundedCornerShape(dimens.radiusS)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("daily:${date.toEpochDay()}")
            .clip(shape)
            .background(background)
            // Today gets an outline rather than a fill, so it can say "today" and "solved" at
            // the same time. A calendar where today is indistinguishable is a list of numbers.
            .then(if (isToday) Modifier.border(dimens.gridBoxLine, colors.accent, shape) else Modifier)
            .clickable(enabled = mark != DayMark.FUTURE, onClick = onClick)
            .alpha(if (mark == DayMark.FUTURE) 0.35f else 1f)
            // One description for the square, not one for the number inside it. A screen
            // reader that says "fourteen" and nothing else is no use on a calendar.
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = date.dayOfMonth.toString(), style = Sendoku.type.label, color = ink)
    }
}

/** What a square on the calendar is. */
internal enum class DayMark { SOLVED, ATTEMPTED, UNPLAYED, FUTURE }

internal fun markFor(date: LocalDate, today: LocalDate, days: DailyDays): DayMark {
    val epochDay = date.toEpochDay()
    return when {
        epochDay in days.solved -> DayMark.SOLVED
        epochDay in days.attempted -> DayMark.ATTEMPTED
        date.isAfter(today) -> DayMark.FUTURE
        else -> DayMark.UNPLAYED
    }
}

/**
 * How many days in a row, counting back from today.
 *
 * Today not being solved yet does not break the streak, because the day is not over. It is
 * counted from yesterday in that case, which is what every other app does and what a player
 * expects at nine in the morning.
 *
 * Worked out from the set of solved days rather than from a stored counter on purpose. A
 * counter has to be adjusted when the clock changes, and a device whose clock jumps backwards
 * would either break a real streak or invent one. A set of days cannot lie: the worst a wrong
 * clock can do is let somebody play a day early, and the count stays right afterwards.
 */
internal fun dailyStreak(solved: Set<Long>, today: LocalDate): Int {
    if (solved.isEmpty()) return 0
    val todayEpoch = today.toEpochDay()
    var day = when {
        todayEpoch in solved -> todayEpoch
        todayEpoch - 1 in solved -> todayEpoch - 1
        else -> return 0
    }
    var count = 0
    while (day in solved) {
        count++
        day--
    }
    return count
}
