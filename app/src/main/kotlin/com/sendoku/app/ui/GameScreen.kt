package com.sendoku.app.ui

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sendoku.app.R
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintEngine
import com.sendoku.app.game.HintLevel
import com.sendoku.app.game.logicCells
import com.sendoku.app.game.struckCells
import com.sendoku.app.theme.Sendoku
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The playing screen: a header, the board, the pad and the tools.
 *
 * State comes in and events go out. Nothing here decides anything about the game, which is
 * what lets every rule about pencil marks, undo and mistakes be tested without an emulator.
 *
 * The layout has two shapes rather than a dozen breakpoints. When the window is wider than
 * it is tall the pad moves beside the board, because a board that has to shrink to leave
 * room underneath is the single worst thing that happens to a sudoku app in landscape.
 */
@Composable
public fun GameScreen(
    state: GameState,
    onEvent: (GameEvent) -> Unit,
    onNextPuzzle: () -> Unit,
    onHome: () -> Unit,
    onGlossary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var longPressed by remember { mutableStateOf<Int?>(null) }
    var confirmLeaving by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Hint?>(null) }

    // One tick a second is enough for a clock that shows seconds, and it stops the moment
    // the game is paused or finished rather than spinning in the background.
    // A hint describes the board it was asked about. Once the board moves on, it is stale.
    LaunchedEffect(state.cells) { hint = null }

    LaunchedEffect(state.isRunning, state.isOver) {
        while (state.isRunning && !state.isOver) {
            delay(1000)
            onEvent(GameEvent.Tick(1.seconds))
        }
    }

    // Walking away from a finished puzzle costs nothing, so only ask when it would. The back
    // gesture and the back button on the screen go through the same decision.
    val leave = {
        if (state.hasProgress && !state.isOver) confirmLeaving = true else onHome()
    }
    BackHandler(enabled = state.hasProgress && !state.isOver) { confirmLeaving = true }

    // The board gives no feedback of its own when a digit lands, so a buzz and a click stand
    // in for the feel of a pencil. Both are only ever fired for a real change, never for a
    // tap that did nothing.
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val feedback: (GameEvent) -> Unit = { event ->
        onEvent(event)
        if (event is GameEvent.Digit || event is GameEvent.Erase) {
            if (state.settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (state.settings.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event -> handleKey(event.key, event.type == KeyEventType.KeyDown, feedback) },
    ) {
        val sideBySide = maxWidth > maxHeight
        // A board wider than this stops being a board and becomes a wall. On a tablet the
        // extra room goes to the margins instead.
        val boardCap = 560.dp

        val content: @Composable () -> Unit = {
            if (sideBySide) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(dimens.spaceM),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
                ) {
                    Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.Center) {
                        BoardArea(state, onEvent, onNextPuzzle, onHome, { longPressed = it }, boardCap, hint)
                    }
                    Column(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM, Alignment.CenterVertically),
                    ) {
                        GameHeader(state, onEvent, leave)
                        HintArea(hint, onEvent, { hint = it }, onGlossary)
                        Controls(state, feedback) { hint = HintEngine.next(state) }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                ) {
                    GameHeader(state, onEvent, leave)
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        BoardArea(state, onEvent, onNextPuzzle, onHome, { longPressed = it }, boardCap, hint)
                    }
                    HintArea(hint, onEvent, { hint = it }, onGlossary)
                    Controls(state, feedback) { hint = HintEngine.next(state) }
                }
            }
        }
        content()

        // Over everything, not just the board. Covering the board alone left the number pad
        // sitting there next to a screen that says tap anywhere to carry on, and in landscape
        // it covered a third of the screen while the rest carried on looking playable.
        if (!state.isOver && !state.isRunning) {
            PauseOverlay(
                elapsed = state.elapsed.clock(),
                onResume = { onEvent(GameEvent.Resume) },
            )
        }
    }

    longPressed?.let { cell ->
        CellActionSheet(
            state = state,
            cell = cell,
            onAction = { event ->
                onEvent(GameEvent.Select(cell))
                feedback(event)
                longPressed = null
            },
            onDismiss = { longPressed = null },
        )
    }

    if (confirmLeaving) {
        AlertDialog(
            onDismissRequest = { confirmLeaving = false },
            containerColor = colors.surfaceRaised,
            title = { Text(stringResource(R.string.leave_title), style = Sendoku.type.title, color = colors.given) },
            text = {
                Text(
                    stringResource(R.string.leave_body),
                    style = Sendoku.type.body,
                    color = colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeaving = false
                    onHome()
                }) {
                    Text(stringResource(R.string.leave_confirm), color = colors.accent, style = Sendoku.type.label)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaving = false }) {
                    Text(stringResource(R.string.leave_cancel), color = colors.muted, style = Sendoku.type.label)
                }
            },
        )
    }
}

@Composable
private fun BoardArea(
    state: GameState,
    onEvent: (GameEvent) -> Unit,
    onNextPuzzle: () -> Unit,
    onHome: () -> Unit,
    onLongPress: (Int) -> Unit,
    cap: androidx.compose.ui.unit.Dp,
    hint: Hint?,
) {
    val step = hint as? Hint.Step
    val showCells = step != null && step.level != HintLevel.NAME
    // The board is square, so it is limited by whichever side is shorter. Sizing it by width
    // alone is right in portrait and wrong in landscape, where it pushed most of the grid off
    // the bottom of the screen.
    BoxWithConstraints {
        val side = minOf(maxWidth, maxHeight, cap)
        Box(Modifier.size(side)) {
            SudokuBoard(
                state = state,
                onSelect = { onEvent(GameEvent.Select(it)) },
                onLongPress = onLongPress,
                hintLogic = if (showCells) step.deduction.logicCells() else emptySet(),
                hintStrike = if (showCells) step.deduction.struckCells() else emptySet(),
                wrong = (hint as? Hint.Mistake)?.cells.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isOver) {
                OutcomePanel(state = state, onNextPuzzle = onNextPuzzle, onHome = onHome)
            }
        }
    }
}

@Composable
private fun Controls(state: GameState, onEvent: (GameEvent) -> Unit, onHint: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceS)) {
        NumberPad(state = state, onDigit = { onEvent(GameEvent.Digit(it)) })
        GameToolbar(
            state = state,
            onUndo = { onEvent(GameEvent.Undo) },
            onRedo = { onEvent(GameEvent.Redo) },
            onErase = { onEvent(GameEvent.Erase) },
            onTogglePencil = { onEvent(GameEvent.TogglePencil) },
            onHint = {
                onEvent(GameEvent.Hint)
                onHint()
            },
        )
    }
}

/** The hint panel, when there is one to show. */
@Composable
private fun HintArea(hint: Hint?, onEvent: (GameEvent) -> Unit, onHint: (Hint?) -> Unit, onGlossary: () -> Unit) {
    if (hint == null) return
    HintPanel(
        hint = hint,
        onMore = { if (hint is Hint.Step) onHint(hint.copy(level = hint.level.next)) },
        onApply = {
            if (hint is Hint.Step) onEvent(GameEvent.Accept(hint.deduction))
            onHint(null)
        },
        onDismiss = { onHint(null) },
        onGlossary = onGlossary,
    )
}

@Composable
private fun GameHeader(state: GameState, onEvent: (GameEvent) -> Unit, onLeave: () -> Unit) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The way out, drawn rather than left to the system gesture. Every other screen
            // has a visible back, and a player who cannot find one on the only screen they
            // spend time in concludes the app has trapped them.
            Text(
                text = stringResource(R.string.back),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onLeave)
                    .padding(dimens.spaceS),
            )
            Text(
                text = stringResource(gradeName(state.grade)).uppercase(),
                style = Sendoku.type.overline,
                color = colors.accent,
                modifier = Modifier.padding(start = dimens.spaceXs),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.settings.mistakeLimit != null) {
                Text(
                    text = stringResource(R.string.mistakes_of, state.mistakes, state.settings.mistakeLimit),
                    style = Sendoku.type.label,
                    color = if (state.mistakes > 0) colors.conflict else colors.muted,
                )
            }
            if (state.settings.showTimer) {
                Text(
                    text = state.elapsed.clock(),
                    style = Sendoku.type.timer,
                    color = colors.muted,
                    modifier = Modifier.clickable { onEvent(GameEvent.Pause) },
                )
            }
        }
    }
}

/**
 * Turns a keypress into a move, for tablets and Chromebooks.
 *
 * A physical keyboard makes a sudoku app dramatically faster to play, and almost nobody
 * supports one. Arrows move, digits fill, backspace clears, space flips into pencil mode.
 */
internal fun handleKey(key: Key, isDown: Boolean, onEvent: (GameEvent) -> Unit): Boolean {
    if (!isDown) return false
    val digit = digitOf(key)
    if (digit != null) {
        onEvent(GameEvent.Digit(digit))
        return true
    }
    val event = when (key) {
        Key.DirectionUp -> GameEvent.Nudge(-1, 0)
        Key.DirectionDown -> GameEvent.Nudge(1, 0)
        Key.DirectionLeft -> GameEvent.Nudge(0, -1)
        Key.DirectionRight -> GameEvent.Nudge(0, 1)
        Key.Backspace, Key.Delete -> GameEvent.Erase
        Key.Spacebar, Key.P -> GameEvent.TogglePencil
        Key.Z, Key.U -> GameEvent.Undo
        Key.Y, Key.R -> GameEvent.Redo
        Key.H -> GameEvent.Hint
        else -> null
    } ?: return false
    onEvent(event)
    return true
}

/** Digits from the number row and from the keypad both count. */
private fun digitOf(key: Key): Int? = when (key) {
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

/** Minutes and seconds, which is the only format a puzzle timer ever needs. */
internal fun Duration.clock(): String {
    val total = inWholeSeconds
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:" + seconds.toString().padStart(2, '0')
}

/** Everything the playing screen can ask for. */
public sealed interface GameEvent {
    public data class Select(val cell: Int) : GameEvent
    public data class Digit(val digit: Int) : GameEvent
    public data class Tick(val delta: Duration) : GameEvent
    public data class Nudge(val rows: Int, val columns: Int) : GameEvent
    public data object Erase : GameEvent
    public data object Undo : GameEvent
    public data object Redo : GameEvent
    public data object TogglePencil : GameEvent
    public data object Hint : GameEvent
    public data object Pause : GameEvent
    public data object Resume : GameEvent
    public data object FillMarks : GameEvent
    public data object ClearMarks : GameEvent

    /** The player accepted a hint and asked the app to carry it out. */
    public data class Accept(val deduction: com.sendoku.engine.technique.Deduction) : GameEvent
}

/** Applies an event to the state. Kept next to the events so neither drifts from the other. */
public fun GameState.reduce(event: GameEvent): GameState = when (event) {
    is GameEvent.Select -> select(event.cell)
    is GameEvent.Digit -> enter(event.digit)
    is GameEvent.Tick -> tick(event.delta)
    is GameEvent.Nudge -> moveSelection(event.rows, event.columns)
    GameEvent.Erase -> erase()
    GameEvent.Undo -> undo()
    GameEvent.Redo -> redo()
    GameEvent.TogglePencil -> togglePencilMode()
    GameEvent.Hint -> countHint()
    GameEvent.Pause -> pause()
    GameEvent.Resume -> resume()
    GameEvent.FillMarks -> fillMarks()
    GameEvent.ClearMarks -> clearMarks()
    is GameEvent.Accept -> applyHint(event.deduction)
}
