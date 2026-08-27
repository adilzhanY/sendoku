package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.data.SavedGame
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuIcons
import com.sendoku.engine.Grade
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration

/** What the home screen needs to know. */
public data class HomeState(
    val solvedByGrade: Map<Grade, Int>,
    val inProgress: InProgressSummary?,
    /** How many days of daily puzzles in a row, so the tile can say whether one is going. */
    val streak: Int = 0,
    val today: LocalDate = LocalDate.now(),
)

/**
 * The puzzle waiting to be picked up, if there is one.
 *
 * The two board strings are the position, flattened the same way the database holds it: the
 * clues it was dealt with, and what the player has put in since. They are here so the home
 * screen can draw the board small, which is how somebody recognises the puzzle they left
 * without reading a word.
 */
public data class InProgressSummary(
    val grade: Grade,
    val placed: Int,
    val total: Int,
    val elapsed: Duration,
    val givens: String = "",
    val entries: String = "",
) {
    val fraction: Float get() = if (total == 0) 0f else placed.toFloat() / total
}

/**
 * Home, which answers "what was I doing" before it asks "what would you like to do".
 *
 * It used to be eight level rows, all the same size and all the same shape, with the puzzle
 * in progress underneath them and a Resume button underneath that. Somebody coming back to a
 * half finished game had to read the whole menu and scroll past it to reach the one thing
 * they opened the app for, and Resume was said twice on the same screen.
 *
 * So the order is now the order a player wants it in. The game in progress is at the top with
 * a picture of its own board. Under it are the only two other things anybody starts from
 * cold: another puzzle at their level, and today's daily. The levels are still all here, as a
 * strip of chips with the solve counts on them, and the chevron opens them back out into full
 * rows for anybody who wants to read what each one asks for.
 *
 * One level opens at a time, and only by winning the one before it.
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

        // The page is short, and a short page that starts at the top leaves a third of the
        // screen empty under it. The two groups are pushed apart instead: what you came for
        // at the top, and the whole ladder along the bottom where a thumb already is. The
        // minimum height is what lets that happen inside something that can still scroll,
        // which it has to be able to do at a large font scale and with the levels opened out.
        BoxWithConstraints(Modifier.weight(1f)) {
            val room = maxHeight
            Column(
                modifier = Modifier
                    .testTag("home:ladder")
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = room)
                    .padding(horizontal = dimens.spaceM)
                    .padding(bottom = dimens.spaceM),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                    state.inProgress?.let { summary -> ContinueCard(summary, onResume) }

                    Overline(
                        stringResource(
                            if (state.inProgress != null) R.string.home_or_start else R.string.home_choose_level,
                        ),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(dimens.padGap)) {
                        // Filled in when there is nothing to come back to, because then this
                        // is the only thing on the screen worth pressing.
                        LevelTile(
                            grade = open,
                            accent = state.inProgress == null,
                            onClick = { onPlay(open) },
                            modifier = Modifier.weight(1f),
                        )
                        DailyTile(
                            today = state.today,
                            streak = state.streak,
                            onClick = onDaily,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                    // Opened out to begin with when the screen is tall enough to be worth
                    // filling, and folded into chips when it is not. A short phone, or a
                    // large font, gets the page that fits; everybody else gets the page that
                    // says what each level asks of them, in the room that was going spare.
                    Levels(state, open, onPlay, initiallyOpen = room >= TALL)
                }
            }
        }
    }
}

/**
 * Every level, small by default and spelled out on request.
 *
 * The chips are enough to pick one: the name, how many of them have been solved, and whether
 * it is shut. What they cannot carry is the line saying what technique the level asks for, or
 * the word ADVANCED, and both of those are worth reading once. The chevron is where they went.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Levels(state: HomeState, open: Grade, onPlay: (Grade) -> Unit, initiallyOpen: Boolean) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    val heading = stringResource(R.string.home_all_levels)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceS)
            .clip(RoundedCornerShape(dimens.radiusS))
            .clickable { expanded = !expanded }
            .testTag("home:levels:toggle")
            .padding(vertical = dimens.spaceXs)
            .semantics(mergeDescendants = true) {
                contentDescription = heading
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(heading, style = Sendoku.type.overline, color = colors.muted)
        Icon(
            imageVector = SendokuIcons.Forward,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp).rotate(if (expanded) 270f else 90f),
        )
    }

    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            for (grade in Grade.entries) {
                GradeRow(
                    grade = grade,
                    solved = state.solvedByGrade[grade] ?: 0,
                    locked = grade.ordinal > open.ordinal,
                    opensAfter = if (grade.ordinal > open.ordinal) Grade.entries[grade.ordinal - 1] else null,
                    onClick = { onPlay(grade) },
                )
            }
        }
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            for (grade in Grade.entries) {
                GradeChip(
                    grade = grade,
                    solved = state.solvedByGrade[grade] ?: 0,
                    locked = grade.ordinal > open.ordinal,
                    opensAfter = if (grade.ordinal > open.ordinal) Grade.entries[grade.ordinal - 1] else null,
                    onClick = { onPlay(grade) },
                )
            }
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

/** What a level is called out loud, whether it is a chip or a row saying it. */
@Composable
private fun levelDescription(grade: Grade, locked: Boolean, opensAfter: Grade?): String {
    val name = stringResource(gradeName(grade))
    val advanced = stringResource(R.string.grade_advanced_talkback)
    val below = opensAfter?.let { stringResource(gradeName(it)) }
    val detail = if (below != null && locked) {
        stringResource(R.string.grade_locked, below)
    } else {
        stringResource(gradeGate(grade))
    }
    return if (grade.isAdvanced) "$name, $advanced, $detail" else "$name, $detail"
}

/**
 * One level, small.
 *
 * The advanced ones keep their warning as a coloured dot rather than the word, because the
 * word does not fit in a pill and a pill that grows to two lines stops being a pill. The
 * word is still said aloud, and it is still written out in the row underneath the chevron.
 */
@Composable
private fun GradeChip(grade: Grade, solved: Int, locked: Boolean, opensAfter: Grade?, onClick: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val description = levelDescription(grade, locked, opensAfter)

    Row(
        modifier = Modifier
            .testTag("home:grade:${grade.name}")
            .clip(CircleShape)
            .background(colors.surface)
            .clickable(enabled = !locked, onClick = onClick)
            .alpha(if (locked) 0.55f else 1f)
            .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXs)
            .semantics(mergeDescendants = true) {
                if (locked) disabled()
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        if (grade.isAdvanced) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.conflict))
        }
        Text(stringResource(gradeName(grade)), style = Sendoku.type.body, color = colors.given)
        if (locked) {
            Icon(
                imageVector = SendokuIcons.Locked,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(14.dp),
            )
        } else if (solved > 0) {
            Text(solved.toString(), style = Sendoku.type.overline, color = colors.accent)
        }
    }
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
    val description = levelDescription(grade, locked, opensAfter)
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
                contentDescription = description
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

/**
 * The puzzle that is still going, with its own board on it.
 *
 * The whole card is the button. A card that looks like this and needs its Resume aimed at
 * exactly is a card that gets tapped in the middle and does nothing, so the tap is the card
 * and the accent block inside it is what says so.
 */
@Composable
private fun ContinueCard(summary: InProgressSummary, onResume: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val name = stringResource(gradeName(summary.grade))
    val resume = stringResource(R.string.home_resume)
    val placed = stringResource(R.string.home_placed, summary.placed, summary.total)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusL))
            .background(colors.surfaceRaised)
            .clickable(onClick = onResume)
            .testTag("home:continue")
            .padding(dimens.spaceM)
            .semantics(mergeDescendants = true) {
                contentDescription = "$resume, $name, $placed"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.home_in_progress), style = Sendoku.type.overline, color = colors.accent)
            Text(summary.elapsed.clock(), style = Sendoku.type.timer, color = colors.muted)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            if (summary.givens.isNotEmpty()) {
                BoardThumb(summary, Modifier.size(THUMB))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                Text(name, style = Sendoku.type.title, color = colors.given)
                Text(placed, style = Sendoku.type.body, color = colors.muted)
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
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimens.minTouchTarget)
                .clip(RoundedCornerShape(dimens.radiusM))
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(resume, style = Sendoku.type.label, color = colors.onAccent)
        }
    }
}

/**
 * The board in progress, drawn small.
 *
 * Digits rather than blocks, because the point is that it is recognisably the grid the player
 * left rather than a bar chart of how much of it is done. The size is worked out from the
 * width in pixels rather than in scaled units, so a phone set to a large font does not blow
 * the digits out of their cells.
 */
@Composable
private fun BoardThumb(summary: InProgressSummary, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dims = remember(summary.givens.length) { SavedGame.dimensionsFor(summary.givens.length) }
    val side = dims.size

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.surface)
            .border(1.dp, colors.boxLine, RoundedCornerShape(4.dp)),
    ) {
        val cell = maxWidth / side
        val text = with(LocalDensity.current) { (cell.toPx() * 0.66f).toSp() }
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until side) {
                Row(Modifier.weight(1f)) {
                    for (column in 0 until side) {
                        val index = row * side + column
                        val given = summary.givens[index]
                        val entered = summary.entries[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if ((row / dims.boxHeight + column / dims.boxWidth) % 2 == 0) {
                                        Color.Transparent
                                    } else {
                                        colors.peer
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            val digit = if (given != EMPTY) given else entered
                            if (digit != EMPTY) {
                                Text(
                                    text = digit.toString(),
                                    style = Sendoku.type.body.copy(fontSize = text),
                                    color = if (given != EMPTY) colors.given else colors.entry,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Another puzzle at the level the player is on, which is the commonest cold start there is. */
@Composable
private fun LevelTile(grade: Grade, accent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Tile(
        overline = stringResource(R.string.home_your_level),
        title = stringResource(gradeName(grade)),
        detail = stringResource(R.string.home_new_puzzle),
        accent = accent,
        tag = "home:new",
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Today's daily, with the streak on it.
 *
 * The streak is the whole reason a daily is worth having, and it used to live two screens
 * away on the calendar. A number that says four days in a row is a reason to open the app
 * tomorrow; a grey button that says Daily is not.
 */
@Composable
private fun DailyTile(today: LocalDate, streak: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(today, locale) {
        today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    Tile(
        overline = stringResource(R.string.home_daily),
        title = date,
        detail = if (streak == 0) {
            stringResource(R.string.daily_play_today)
        } else {
            pluralStringResource(R.plurals.daily_streak, streak, streak)
        },
        accent = false,
        tag = "home:daily",
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun Tile(
    overline: String,
    title: String,
    detail: String,
    accent: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val ink = if (accent) colors.onAccent else colors.given
    val quiet = if (accent) colors.onAccent.copy(alpha = 0.7f) else colors.muted

    Column(
        modifier = modifier
            .heightIn(min = dimens.minTouchTarget)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(if (accent) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(dimens.spaceM)
            .semantics(mergeDescendants = true) {
                contentDescription = "$overline, $title, $detail"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(overline.shout(), style = Sendoku.type.overline, color = quiet)
        Text(title, style = Sendoku.type.label, color = ink)
        Text(detail, style = Sendoku.type.body, color = if (accent) quiet else colors.accent)
    }
}

@Composable
private fun Overline(text: String) {
    Text(
        text = text,
        style = Sendoku.type.overline,
        color = Sendoku.colors.muted,
        modifier = Modifier.padding(top = Sendoku.dimens.spaceS),
    )
}

private const val EMPTY = '.'

/** Tall enough that the levels are better read than folded away. */
private val TALL = 700.dp
private val THUMB = 92.dp
