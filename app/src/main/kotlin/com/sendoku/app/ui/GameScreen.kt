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
    onGlossary: (com.sendoku.engine.technique.TechniqueId?) -> Unit,
    onSettings: () -> Unit,
    onPath: () -> Unit,
    /** Records what was asked about, so the stats can say which rule a player leans on. */
    onSpend: (com.sendoku.engine.technique.TechniqueId, HintLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    var longPressed by remember { mutableStateOf<Int?>(null) }
    var confirmLeaving by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Hint?>(null) }

    // The chooser in front of a hint, and the answer to the free check when it has been
    // asked for. Both belong to the screen rather than the game: neither changes the board.
    var menuOpen by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf<Int?>(null) }

    // One tick a second is enough for a clock that shows seconds, and it stops the moment
    // the game is paused or finished rather than spinning in the background.
    // A hint describes the board it was asked about. Once the board moves on, it is stale.
    LaunchedEffect(state.cells) {
        hint = null
        checked = null
    }

    // And a finished game has nothing left to hint at. Without this the panel sat under the
    // result still offering to show you where, on a board that was already over.
    LaunchedEffect(state.isOver) {
        if (state.isOver) {
            hint = null
            menuOpen = false
        }
    }

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
                        BoardArea(state, onEvent, onNextPuzzle, onHome, {
                            longPressed = it
                        }, boardCap, hint, onGlossary, onPath)
                    }
                    Column(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM, Alignment.CenterVertically),
                    ) {
                        GameHeader(state, leave, onSettings, onPause = { onEvent(GameEvent.Pause) })
                        HintArea(hint, onEvent, { hint = it }, onGlossary)
                        HintMenuArea(state, menuOpen, checked, { checked = it }, { menuOpen = it }) {
                            hint = askForHint(state, hint, it, feedback, onSpend)
                        }
                        Controls(state, feedback) { menuOpen = true }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                ) {
                    GameHeader(state, leave, onSettings, onPause = { onEvent(GameEvent.Pause) })
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        BoardArea(state, onEvent, onNextPuzzle, onHome, {
                            longPressed = it
                        }, boardCap, hint, onGlossary, onPath)
                    }
                    HintArea(hint, onEvent, { hint = it }, onGlossary)
                    HintMenuArea(state, menuOpen, checked, { checked = it }, { menuOpen = it }) {
                        hint = askForHint(state, hint, it, feedback, onSpend)
                    }
                    Controls(state, feedback) { menuOpen = true }
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
    onLearn: (com.sendoku.engine.technique.TechniqueId) -> Unit,
    onPath: () -> Unit,
) {
    val step = hint as? Hint.Step
    // The cells come out one level later than the region does. The quiet level exists to say
    // where to look, and lighting up the cells there would hand over the whole answer.
    val showCells = step != null && (step.level == HintLevel.CELLS || step.level == HintLevel.FULL)
    val showRegion = step != null && step.level != HintLevel.NAME
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
                hintHouses = if (showRegion) step.deduction.houses else emptyList(),
                struckMarks = if (showCells) step.deduction.eliminations.toSet() else emptySet(),
                spotlight = true,
                // Auto check puts the same red under a digit the answer does not want, the
                // moment it goes in, rather than only when a hint is asked for.
                wrong = (hint as? Hint.Mistake)?.cells.orEmpty() + state.flaggedWrong,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isOver) {
                OutcomePanel(
                    state = state,
                    onNextPuzzle = onNextPuzzle,
                    onHome = onHome,
                    onLearn = onLearn,
                    onPath = onPath,
                )
            }
        }
    }
}

/**
 * The next hint, and whether it costs one.
 *
 * Tapping the button again while the same hint is still on screen is free. It has to be:
 * hints are limited, and the panel is easy to lose track of on a small screen, so charging
 * for a second look at something the app has already said would end games by accident. A
 * hint costs when it tells the player something they have not been told yet.
 */
private fun askForHint(
    state: GameState,
    showing: Hint?,
    level: HintLevel,
    onEvent: (GameEvent) -> Unit,
    onSpend: (com.sendoku.engine.technique.TechniqueId, HintLevel) -> Unit,
): Hint {
    val next = HintEngine.next(state, level)
    val alreadySaid = when {
        showing == null -> false
        showing is Hint.Step && next is Hint.Step -> showing.deduction == next.deduction
        else -> showing == next
    }
    if (alreadySaid) {
        // Keep whatever level the player had already unfolded, or the panel would fold
        // itself back up under them.
        return checkNotNull(showing)
    }
    onEvent(GameEvent.Hint)
    if (next is Hint.Step) onSpend(next.deduction.technique, level)
    return next
}

@Composable
private fun Controls(state: GameState, onEvent: (GameEvent) -> Unit, onHint: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceS)) {
        NumberPad(
            state = state,
            onDigit = { onEvent(GameEvent.Digit(it)) },
            onScan = { onEvent(GameEvent.Scan(it)) },
        )
        GameToolbar(
            state = state,
            onUndo = { onEvent(GameEvent.Undo) },
            onRedo = { onEvent(GameEvent.Redo) },
            onErase = { onEvent(GameEvent.Erase) },
            onTogglePencil = { onEvent(GameEvent.TogglePencil) },
            onFillNotes = { onEvent(GameEvent.FillAllMarks) },
            onHint = onHint,
        )
    }
}

/**
 * The chooser, when the hint button has been pressed and nothing has been asked for yet.
 *
 * Kept beside the panel rather than inside it, because one of them costs a hint and the
 * other does not, and a player has to be able to see which is which.
 */
@Composable
private fun HintMenuArea(
    state: GameState,
    open: Boolean,
    checked: Int?,
    onChecked: (Int?) -> Unit,
    onOpen: (Boolean) -> Unit,
    onAsk: (HintLevel) -> Unit,
) {
    if (!open) return
    HintMenu(
        state = state,
        checked = checked,
        onCheck = { onChecked(state.wrongSoFar) },
        onLook = {
            onOpen(false)
            onAsk(HintLevel.REGION)
        },
        onExplain = {
            onOpen(false)
            onAsk(state.settings.hintDetail)
        },
        onDismiss = {
            onOpen(false)
            onChecked(null)
        },
    )
}

/** The hint panel, when there is one to show. */
@Composable
private fun HintArea(
    hint: Hint?,
    onEvent: (GameEvent) -> Unit,
    onHint: (Hint?) -> Unit,
    onGlossary: (com.sendoku.engine.technique.TechniqueId?) -> Unit,
) {
    if (hint == null) return
    HintPanel(
        hint = hint,
        onMore = { if (hint is Hint.Step) onHint(hint.copy(level = hint.level.next)) },
        onApply = {
            if (hint is Hint.Step) onEvent(GameEvent.Accept(hint.deduction))
            onHint(null)
        },
        onDismiss = { onHint(null) },
        onGlossary = { onGlossary((hint as? Hint.Step)?.deduction?.technique) },
        onRemoveMistake = {
            if (hint is Hint.Mistake) onEvent(GameEvent.EraseCells(hint.cells))
            onHint(null)
        },
    )
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

    /** The player is holding a digit up to the board to see where it could go. */
    public data class Scan(val digit: Int) : GameEvent

    public data object FillMarks : GameEvent

    /** Pencil every empty cell in, which is the one piece of help the deep end needs. */
    public data object FillAllMarks : GameEvent
    public data object ClearMarks : GameEvent

    /** The player asked the hint to take the wrong digits it found back off the board. */
    public data class EraseCells(val cells: Set<Int>) : GameEvent

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
    is GameEvent.EraseCells -> eraseAll(event.cells)
    GameEvent.Undo -> undo()
    GameEvent.Redo -> redo()
    GameEvent.TogglePencil -> togglePencilMode()
    GameEvent.Hint -> countHint()
    GameEvent.Pause -> pause()
    GameEvent.Resume -> resume()
    is GameEvent.Scan -> scanFor(event.digit)
    GameEvent.FillMarks -> fillMarks()
    GameEvent.FillAllMarks -> fillAllMarks()
    GameEvent.ClearMarks -> clearMarks()
    is GameEvent.Accept -> applyHint(event.deduction)
}
