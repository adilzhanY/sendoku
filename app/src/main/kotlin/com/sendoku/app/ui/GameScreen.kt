package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sendoku.app.game.GameState
import com.sendoku.app.theme.Sendoku
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * The playing screen: a header, the board, the pad and the tools.
 *
 * State comes in and events go out. Nothing here decides anything about the game, which is
 * what lets every rule about pencil marks, undo and mistakes be tested without an emulator.
 */
@Composable
public fun GameScreen(
    state: GameState,
    onEvent: (GameEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    // One tick a second is enough for a clock that shows seconds, and it stops the moment
    // the game is paused or finished rather than spinning in the background.
    LaunchedEffect(state.isRunning, state.isOver) {
        while (state.isRunning && !state.isOver) {
            delay(1000)
            onEvent(GameEvent.Tick(1.seconds))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        GameHeader(state, onEvent)

        Box(Modifier.fillMaxWidth()) {
            SudokuBoard(
                state = state,
                onSelect = { onEvent(GameEvent.Select(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!state.isRunning && !state.isOver) {
                PauseOverlay(
                    elapsed = state.elapsed.clock(),
                    onResume = { onEvent(GameEvent.Resume) },
                )
            }
        }

        if (state.isOver) {
            GameOutcome(state)
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            NumberPad(state = state, onDigit = { onEvent(GameEvent.Digit(it)) })
            GameToolbar(
                state = state,
                onUndo = { onEvent(GameEvent.Undo) },
                onRedo = { onEvent(GameEvent.Redo) },
                onErase = { onEvent(GameEvent.Erase) },
                onTogglePencil = { onEvent(GameEvent.TogglePencil) },
                onHint = { onEvent(GameEvent.Hint) },
            )
        }
    }
}

@Composable
private fun GameHeader(state: GameState, onEvent: (GameEvent) -> Unit) {
    val colors = Sendoku.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.grade.displayName.uppercase(),
            style = Sendoku.type.overline,
            color = colors.accent,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.settings.mistakeLimit != null) {
                Text(
                    text = "${state.mistakes} of ${state.settings.mistakeLimit}",
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

@Composable
private fun GameOutcome(state: GameState) {
    val colors = Sendoku.colors
    Text(
        text = if (state.isSolved) {
            "Solved in ${state.elapsed.clock()}"
        } else {
            "Out of mistakes"
        },
        style = Sendoku.type.title,
        color = if (state.isSolved) colors.accent else colors.conflict,
    )
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
    public data object Erase : GameEvent
    public data object Undo : GameEvent
    public data object Redo : GameEvent
    public data object TogglePencil : GameEvent
    public data object Hint : GameEvent
    public data object Pause : GameEvent
    public data object Resume : GameEvent
}

/** Applies an event to the state. Kept next to the events so neither drifts from the other. */
public fun GameState.reduce(event: GameEvent): GameState = when (event) {
    is GameEvent.Select -> select(event.cell)
    is GameEvent.Digit -> enter(event.digit)
    is GameEvent.Tick -> tick(event.delta)
    GameEvent.Erase -> erase()
    GameEvent.Undo -> undo()
    GameEvent.Redo -> redo()
    GameEvent.TogglePencil -> togglePencilMode()
    GameEvent.Hint -> countHint()
    GameEvent.Pause -> pause()
    GameEvent.Resume -> resume()
}
