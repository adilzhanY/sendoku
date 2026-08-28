package com.sendoku.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendoku.app.data.Dealt
import com.sendoku.app.data.GameRepository
import com.sendoku.app.data.PuzzleSource
import com.sendoku.app.data.SettingsStore
import com.sendoku.app.ui.GameEvent
import com.sendoku.app.ui.Verdict
import com.sendoku.app.ui.reduce
import com.sendoku.engine.Board
import com.sendoku.engine.Grade
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Solver
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.PuzzleRef
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.TechniqueSolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The line between the game and the screen.
 *
 * Everything with a rule in it lives in [GameState], which is immutable and has no Android
 * in it. This holds the current one, writes it down, and owns the coroutines. Nothing here
 * decides what a move does.
 */
@OptIn(FlowPreview::class)
public class GameViewModel(
    private val repository: GameRepository,
    private val settingsStore: SettingsStore,
    private val puzzles: PuzzleSource,
    /**
     * Where the coroutines run.
     *
     * Null in the app, where [viewModelScope] is correct: it is cancelled when the view
     * model is finally cleared rather than when the activity is recreated, which is the
     * whole reason a rotation no longer throws the game away. Tests pass their own.
     */
    externalScope: CoroutineScope? = null,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _state = MutableStateFlow<GameState?>(null)
    public val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _loading = MutableStateFlow(true)
    public val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        scope.launch {
            // Saving on literally every move would write a row per keystroke. Waiting for a
            // pause writes once per burst, and the pause is short enough that anything short
            // of the process being killed mid-tap is caught.
            _state.filterNotNull()
                .debounce(SAVE_AFTER_MILLIS)
                .collect { repository.saveInProgress(it) }
        }
    }

    /** Picks up where the player left off, or starts something new at [fallbackGrade]. */
    public fun resumeOrStart(fallbackGrade: Grade = Grade.GENTLE) {
        scope.launch {
            _loading.value = true
            val settings = settingsStore.settings.first()
            val resumed = repository.loadInProgress(settings)
            _state.value = resumed ?: puzzles.next(fallbackGrade).play(settings)
            _loading.value = false
        }
    }

    /** Today's puzzle, the one everybody on this version gets. */
    public fun startDaily(epochDay: Long) {
        scope.launch {
            _loading.value = true
            val settings = settingsStore.settings.first()
            _state.value = puzzles.daily(epochDay).play(settings, dailyEpochDay = epochDay)
            _loading.value = false
        }
    }

    /**
     * Starts the puzzle a share code names, or reports why it could not.
     *
     * A code is the one way a puzzle arrives from outside this phone, so it is also the one
     * place the app has to be careful. A code that reads correctly can still name a puzzle
     * this build does not have, and a grid that arrives in full can still be unsolvable, have
     * two answers, or need reasoning past the end of the ladder. All three come back as a
     * refusal with a reason rather than as a board that behaves strangely later.
     */
    public fun startShared(ref: PuzzleRef, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            _loading.value = true
            val settings = settingsStore.settings.first()
            val found = puzzles.byCode(ref)
            if (found != null) {
                _state.value = found.play(settings, origin = PuzzleOrigin.SHARED)
            }
            _loading.value = false
            onResult(found != null)
        }
    }

    /**
     * Starts a puzzle that turns on [technique], and says whether there was one.
     *
     * It plays as an ordinary game at whatever grade the puzzle turns out to be, because it
     * is one: a puzzle chosen for the rule it needs is still a puzzle off the ladder, and
     * winning it counts for exactly as much as winning any other.
     */
    public fun startNeeding(technique: TechniqueId, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            _loading.value = true
            val settings = settingsStore.settings.first()
            val found = puzzles.needing(technique)
            if (found != null) _state.value = found.play(settings)
            _loading.value = false
            onResult(found != null)
        }
    }

    /** How many puzzles in the batch turn on each technique. */
    public suspend fun techniqueSupply(): Map<TechniqueId, Int> = puzzles.supply()

    /**
     * Judges a grid somebody typed in.
     *
     * Off the main thread, because counting the answers to a nearly empty grid is the one
     * piece of work in this app that can take a noticeable moment, and a screen that stops
     * responding while it thinks is a screen people tap again.
     */
    public fun check(givens: Board, onVerdict: (Verdict) -> Unit) {
        scope.launch {
            onVerdict(withContext(Dispatchers.Default) { judge(givens) })
        }
    }

    /**
     * Starts a puzzle that was typed in.
     *
     * It counts in the history and in the statistics, and it opens no levels, for the same
     * reason a shared puzzle does not: it is a puzzle the player brought, not one they
     * climbed to.
     */
    public fun startEntered(givens: Board, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            _loading.value = true
            val settings = settingsStore.settings.first()
            val rated = withContext(Dispatchers.Default) { rate(givens) }
            if (rated != null) {
                _state.value = GameState.start(rated, settings, origin = PuzzleOrigin.ENTERED)
            }
            _loading.value = false
            onResult(rated != null)
        }
    }

    public fun startNew(grade: Grade) {
        scope.launch {
            _loading.value = true
            val settings = settingsStore.settings.first()
            _state.value = puzzles.next(grade).play(settings)
            _loading.value = false
        }
    }

    public fun onEvent(event: GameEvent) {
        val current = _state.value ?: return
        val next = current.reduce(event)
        if (next === current) return
        _state.value = next

        // A finished game moves straight into the history, before anything can be undone
        // back out of it.
        if (next.isOver && !current.isOver) {
            scope.launch { repository.recordFinished(next, now()) }
        }
    }

    /** Writes the current game down now, rather than waiting for the pause. */
    public fun saveNow() {
        val current = _state.value ?: return
        scope.launch { repository.saveInProgress(current) }
    }

    public fun pause() {
        onEvent(GameEvent.Pause)
        saveNow()
    }

    /**
     * Who stopped the clock: the system, or the player.
     *
     * Lives here rather than in the activity because a rotation throws the activity away and
     * keeps this. An activity flag was always false on the way back in, so every rotation
     * left the game paused.
     */
    private var clockStoppedByBackground = false

    /** The app left the screen. Stop the clock, and remember that we were the ones who did. */
    public fun onBackground() {
        clockStoppedByBackground = _state.value?.isRunning == true
        pause()
    }

    /** The app came back. Start the clock again, unless the player paused it deliberately. */
    public fun onForeground() {
        if (!clockStoppedByBackground) return
        clockStoppedByBackground = false
        onEvent(GameEvent.Resume)
    }

    /**
     * What the app can honestly say about a grid it did not deal.
     *
     * Three questions in order, because the answers only make sense that way. Does it have
     * an answer at all, does it have exactly one, and can the ladder reach that one by
     * reasoning. A grid can fail the last of those and still be a perfectly good sudoku, and
     * saying so is more honest than pretending the hint engine could carry it.
     */
    private fun judge(givens: Board): Verdict {
        val solutions = Solver(givens.dims).countSolutions(givens, limit = 2)
        return when {
            solutions == 0 -> Verdict.Impossible

            solutions > 1 -> Verdict.Ambiguous

            else -> {
                val report = TechniqueSolver().solve(givens.copy())
                if (report.isSolved) Verdict.Ready(report.grade, report.rating) else Verdict.BeyondTheLadder
            }
        }
    }

    /** A typed grid as something playable, or null when it has no single answer. */
    private fun rate(givens: Board): RatedPuzzle? {
        val solution = Solver(givens.dims).solve(givens) ?: return null
        val report = TechniqueSolver().solve(givens.copy())
        return RatedPuzzle(
            puzzle = Puzzle(givens = givens, solution = solution),
            rating = report.rating,
            grade = report.grade,
            hardest = report.hardest,
            symmetry = Symmetry.NONE,
            usage = report.usage,
        )
    }

    /** A dealt puzzle, made playable. Keeps the batch index so the game can be shared short. */
    private fun Dealt.play(
        settings: GameSettings,
        dailyEpochDay: Long? = null,
        origin: PuzzleOrigin = if (dailyEpochDay != null) PuzzleOrigin.DAILY else PuzzleOrigin.LADDER,
    ): GameState = GameState.start(
        rated = puzzle,
        settings = settings,
        dailyEpochDay = dailyEpochDay,
        origin = origin,
        catalogIndex = catalogIndex,
    )

    public companion object {
        internal const val SAVE_AFTER_MILLIS = 400L
    }
}
