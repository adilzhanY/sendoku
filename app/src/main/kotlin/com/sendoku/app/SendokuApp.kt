package com.sendoku.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.DailyDays
import com.sendoku.app.data.HintLog
import com.sendoku.app.data.Statistics
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameViewModel
import com.sendoku.app.game.SolvePath
import com.sendoku.app.learn.CourseProgress
import com.sendoku.app.learn.CourseScreen
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.learn.LessonId
import com.sendoku.app.learn.LessonPlayer
import com.sendoku.app.learn.PracticeHost
import com.sendoku.app.learn.PracticePlan
import com.sendoku.app.learn.met
import com.sendoku.app.nav.Destination
import com.sendoku.app.nav.Navigator
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.ui.AboutScreen
import com.sendoku.app.ui.AccountScreen
import com.sendoku.app.ui.BottomBar
import com.sendoku.app.ui.CodeMiss
import com.sendoku.app.ui.DailyScreen
import com.sendoku.app.ui.GameScreen
import com.sendoku.app.ui.GlossaryScreen
import com.sendoku.app.ui.HistoryGameScreen
import com.sendoku.app.ui.HistoryScreen
import com.sendoku.app.ui.HomeScreen
import com.sendoku.app.ui.HomeState
import com.sendoku.app.ui.InProgressSummary
import com.sendoku.app.ui.LicencesScreen
import com.sendoku.app.ui.ReadableWidth
import com.sendoku.app.ui.SettingsScreen
import com.sendoku.app.ui.SolvePathScreen
import com.sendoku.app.ui.StatsScreen
import com.sendoku.app.ui.dailyStreak
import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.CodeFault
import com.sendoku.engine.catalog.CodeResult
import com.sendoku.engine.catalog.PuzzleCode
import com.sendoku.engine.catalog.PuzzleRef
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The whole app, one screen at a time.
 *
 * A single activity with a back stack of typed destinations. Home is always underneath, so
 * the system back gesture always has somewhere to go, and only the playing screen ever
 * intercepts it, to ask before throwing away a half finished puzzle.
 */
@Composable
public fun SendokuApp(
    model: GameViewModel,
    settings: Flow<GameSettings>,
    onSettingsChange: (GameSettings) -> Unit,
    solvedByGrade: Flow<Map<Grade, Int>>,
    savedGame: Flow<InProgressSummary?>,
    statistics: Flow<Statistics>,
    /** Every finished game, newest first, for the history screen. */
    history: Flow<List<com.sendoku.app.data.FinishedGame>>,
    dailyDays: Flow<DailyDays>,
    course: Flow<CourseProgress>,
    onLessonStep: (LessonId, Int, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onResetCourse: () -> Unit,
    dataMessage: String?,
    onPractice: (TechniqueId, Boolean) -> Unit,
    puzzles: () -> Sequence<com.sendoku.engine.catalog.RatedPuzzle>,
    appearance: Flow<Appearance>,
    onAppearanceChange: (Appearance) -> Unit,
    onResetStats: () -> Unit,
    /** Records which rule a hint was about, for the stats screen. Nothing leaves the phone. */
    onSpendHint: (com.sendoku.engine.technique.TechniqueId, com.sendoku.app.game.HintLevel) -> Unit,
    hintLog: Flow<HintLog>,
    version: String,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    /** A code the app was opened with, or null when it was opened the ordinary way. */
    opening: String? = null,
    onOpened: () -> Unit = {},
) {
    val navigator = rememberSaveable(saver = Navigator.Saver) { Navigator() }
    val loading by model.loading.collectAsState()
    val counts by solvedByGrade.collectAsState(initial = emptyMap())
    val saved by savedGame.collectAsState(initial = null)
    val stats by statistics.collectAsState(initial = Statistics.of(emptyList()))
    val played by history.collectAsState(initial = emptyList())
    val calendar by dailyDays.collectAsState(initial = DailyDays())
    val learning by course.collectAsState(initial = CourseProgress())
    val currentSettings by settings.collectAsState(initial = GameSettings())
    val look by appearance.collectAsState(initial = Appearance())
    val hintTally by hintLog.collectAsState(initial = HintLog())

    // Back from anywhere except home goes back a screen. Home itself lets the system take it,
    // because the way out of a home screen is out of the app.
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    // A link opens a puzzle once. It is read through the same reader a pasted code goes
    // through, so a link that names nothing lands on the home screen with the same sentence
    // rather than on a blank board.
    var linkMiss by rememberSaveable { mutableStateOf<CodeMiss?>(null) }
    var linkFault by rememberSaveable { mutableStateOf<CodeFault?>(null) }
    LaunchedEffect(opening) {
        val code = opening ?: return@LaunchedEffect
        onOpened()
        when (val result = PuzzleCode.read(code)) {
            is CodeResult.Failed -> {
                linkFault = result.fault
                navigator.home()
            }

            is CodeResult.Ok -> model.startShared(result.ref) { opened ->
                if (opened) {
                    navigator.go(Destination.Shared(code))
                } else {
                    linkMiss = if (result.ref is PuzzleRef.Batch) {
                        CodeMiss.NOT_IN_THIS_VERSION
                    } else {
                        CodeMiss.CANNOT_BE_REASONED
                    }
                    navigator.home()
                }
            }
        }
    }

    ReadableWidth(modifier) { pane ->
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                Screens(
                    navigator = navigator,
                    model = model,
                    scope = scope,
                    counts = counts,
                    linkFault = linkFault,
                    linkMiss = linkMiss,
                    saved = saved,
                    stats = stats,
                    played = played,
                    currentSettings = currentSettings,
                    look = look,
                    loading = loading,
                    dailyDays = calendar,
                    course = learning,
                    onLessonStep = onLessonStep,
                    onExport = onExport,
                    onImport = onImport,
                    onResetCourse = onResetCourse,
                    dataMessage = dataMessage,
                    onPractice = onPractice,
                    puzzles = puzzles,
                    settingsChange = onSettingsChange,
                    appearanceChange = onAppearanceChange,
                    resetStats = onResetStats,
                    onSpendHint = onSpendHint,
                    hints = hintTally,
                    version = version,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Only on the three roots. A puzzle or a lesson is somewhere you are in the middle
            // of something, and a bar offering to leave is furniture in the way.
            if (navigator.atRoot) {
                BottomBar(current = navigator.current, onSelect = { navigator.switchTo(it) })
            }
        }
        // Silences the unused warning on the pane modifier while the bar owns the width.
        pane.let { }
    }
}

@Composable
private fun Screens(
    navigator: Navigator,
    model: GameViewModel,
    scope: CoroutineScope,
    counts: Map<Grade, Int>,
    /** What went wrong with a code the app was opened by, if anything did. */
    linkFault: CodeFault?,
    linkMiss: CodeMiss?,
    saved: InProgressSummary?,
    stats: Statistics,
    played: List<com.sendoku.app.data.FinishedGame>,
    currentSettings: GameSettings,
    look: Appearance,
    loading: Boolean,
    dailyDays: DailyDays,
    course: CourseProgress,
    onLessonStep: (LessonId, Int, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onResetCourse: () -> Unit,
    dataMessage: String?,
    onPractice: (TechniqueId, Boolean) -> Unit,
    puzzles: () -> Sequence<com.sendoku.engine.catalog.RatedPuzzle>,
    settingsChange: (GameSettings) -> Unit,
    appearanceChange: (Appearance) -> Unit,
    resetStats: () -> Unit,
    onSpendHint: (TechniqueId, com.sendoku.app.game.HintLevel) -> Unit,
    hints: HintLog,
    version: String,
    modifier: Modifier = Modifier,
) {
    when (val here = navigator.current) {
        Destination.Home -> {
            val today = remember { java.time.LocalDate.now() }
            // Cleared on every attempt, so a message never outlives the code it was about.
            var codeFault by rememberSaveable { mutableStateOf<CodeFault?>(null) }
            var codeMiss by rememberSaveable { mutableStateOf<CodeMiss?>(null) }
            // A link that failed says so here too, since this is where the player lands.
            val shownFault = codeFault ?: linkFault
            val shownMiss = codeMiss ?: linkMiss
            HomeScreen(
                state = HomeState(
                    solvedByGrade = counts,
                    // Read from storage, not from the live game. On a cold start there is no
                    // live game yet, and the home screen would say there is nothing to resume.
                    inProgress = saved,
                    // The daily tile says how many days in a row, which is the only reason a
                    // daily is worth having and used to be two screens away on the calendar.
                    streak = dailyStreak(dailyDays.solved, today),
                    today = today,
                ),
                // Starting a game is an effect of navigating, not of drawing. Doing it in a
                // LaunchedEffect on the play screen meant that coming back from the glossary
                // recomposed it and silently dealt a brand new puzzle.
                onPlay = { grade ->
                    model.startNew(grade)
                    navigator.go(Destination.Play(grade))
                },
                onResume = {
                    model.resumeOrStart()
                    navigator.go(Destination.Resume)
                },
                // The calendar, not straight into today. A daily is only worth having if a
                // missed day is visible and a caught up day is possible, and both live there.
                onDaily = { navigator.go(Destination.Calendar) },
                // A code is the one thing that arrives from outside the phone, so it is read
                // here rather than inside the screen: the screen shows what went wrong, and
                // this decides whether anything did.
                fault = shownFault,
                miss = shownMiss,
                onCode = { text ->
                    codeFault = null
                    codeMiss = null
                    when (val result = PuzzleCode.read(text)) {
                        is CodeResult.Failed -> codeFault = result.fault

                        is CodeResult.Ok -> model.startShared(result.ref) { opened ->
                            if (opened) {
                                navigator.go(Destination.Shared(text))
                            } else {
                                // A code that reads and opens nothing is two different
                                // problems: a batch this build does not have, or a grid the
                                // ladder cannot finish. Only one of them is worth resending.
                                codeMiss = if (result.ref is PuzzleRef.Batch) {
                                    CodeMiss.NOT_IN_THIS_VERSION
                                } else {
                                    CodeMiss.CANNOT_BE_REASONED
                                }
                            }
                        }
                    }
                },
                modifier = modifier,
            )
        }

        is Destination.Play, Destination.Resume, is Destination.Daily, is Destination.Shared ->
            PlayHost(model, loading, navigator, scope, onSpendHint, modifier)

        Destination.Calendar -> {
            DailyScreen(
                today = java.time.LocalDate.now(),
                days = dailyDays,
                onPlay = { epochDay ->
                    model.startDaily(epochDay)
                    navigator.go(Destination.Daily(epochDay))
                },
                onBack = { navigator.back() },
                modifier = modifier,
            )
        }

        Destination.Course -> {
            CourseScreen(
                progress = course,
                onOpen = { navigator.go(Destination.LessonAt(it.name)) },
                onPractise = { navigator.go(Destination.Practice("")) },
                onBack = if (navigator.canGoBack) ({ navigator.back() }) else null,
                modifier = modifier,
            )
        }

        is Destination.LessonAt -> {
            val id = runCatching { LessonId.valueOf((navigator.current as Destination.LessonAt).lesson) }
                .getOrDefault(LessonId.WHAT_A_SUDOKU_IS)
            val lesson = Curriculum.byId(id)
            LessonPlayer(
                lesson = lesson,
                startAt = course.lessons[id]?.step ?: 0,
                onStep = { step -> onLessonStep(id, step, false) },
                onFinished = {
                    onLessonStep(id, lesson.steps.lastIndex, true)
                    navigator.back()
                },
                onLeave = { navigator.back() },
                modifier = modifier,
            )
        }

        is Destination.Practice -> {
            val asked = (navigator.current as Destination.Practice).technique
            val technique = runCatching { TechniqueId.valueOf(asked) }.getOrNull()
                ?: PracticePlan.next(course.met(), course.mastery, System.currentTimeMillis())
            PracticeHost(
                technique = technique,
                puzzles = puzzles,
                onAnswer = onPractice,
                onBack = { navigator.back() },
                modifier = modifier,
            )
        }

        Destination.Account -> {
            AccountScreen(
                statistics = stats,
                course = course,
                history = played,
                onStats = { navigator.go(Destination.Stats) },
                onHistory = { navigator.go(Destination.History) },
                onSettings = { navigator.go(Destination.Settings) },
                onAbout = { navigator.go(Destination.About) },
                modifier = modifier,
            )
        }

        is Destination.Path -> {
            val path = remember(here.givens) {
                SolvePath.of(com.sendoku.engine.Board.parse(com.sendoku.engine.Dimensions.CLASSIC, here.givens))
            }
            SolvePathScreen(path = path, onBack = { navigator.back() }, modifier = modifier)
        }

        Destination.History -> {
            HistoryScreen(
                games = played,
                onBack = { navigator.back() },
                onOpen = { navigator.go(Destination.HistoryGame(it.finishedAt)) },
                modifier = modifier,
            )
        }

        is Destination.HistoryGame -> {
            // Found by when it ended, which is the only thing that identifies a game. If it is
            // gone, so is the screen: a reset while the detail is open should not leave the
            // player looking at a game that no longer exists.
            val game = played.firstOrNull { it.finishedAt == here.finishedAt }
            if (game == null) {
                navigator.back()
            } else {
                HistoryGameScreen(
                    game = game,
                    onBack = { navigator.back() },
                    onLearn = { technique ->
                        Curriculum.teaching(technique)?.let { navigator.go(Destination.LessonAt(it.id.name)) }
                    },
                    modifier = modifier,
                )
            }
        }

        Destination.Stats -> {
            StatsScreen(
                statistics = stats,
                hints = hints,
                onBack = { navigator.back() },
                onReset = resetStats,
                modifier = modifier,
            )
        }

        Destination.About -> {
            AboutScreen(
                version = version,
                onBack = { navigator.back() },
                onLicences = { navigator.go(Destination.Licences) },
                modifier = modifier,
            )
        }

        Destination.Licences -> {
            LicencesScreen(onBack = { navigator.back() }, modifier = modifier)
        }

        Destination.Glossary -> {
            GlossaryScreen(
                onBack = { navigator.back() },
                onLesson = { technique ->
                    val lesson = Curriculum.teaching(technique)
                    if (lesson != null) navigator.go(Destination.LessonAt(lesson.id.name))
                },
                modifier = modifier,
            )
        }

        Destination.Settings -> {
            SettingsScreen(
                settings = currentSettings,
                appearance = look,
                onChange = settingsChange,
                onAppearanceChange = appearanceChange,
                onBack = { navigator.back() },
                onAbout = { navigator.go(Destination.About) },
                onExport = onExport,
                onImport = onImport,
                onResetCourse = onResetCourse,
                dataMessage = dataMessage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PlayHost(
    model: GameViewModel,
    loading: Boolean,
    navigator: Navigator,
    scope: CoroutineScope,
    onSpendHint: (com.sendoku.engine.technique.TechniqueId, com.sendoku.app.game.HintLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by model.state.collectAsState()
    val game = state
    if (loading || game == null) {
        Loading(modifier)
        return
    }
    GameScreen(
        state = game,
        onEvent = model::onEvent,
        onNextPuzzle = { scope.launch { model.startNew(game.grade) } },
        onHome = { navigator.home() },
        // A technique with a lesson goes to the lesson. One without, or a tap from somewhere
        // that has no technique in hand, goes to the glossary as before.
        onGlossary = { technique ->
            val lesson = technique?.let { Curriculum.teaching(it) }
            navigator.go(if (lesson == null) Destination.Glossary else Destination.LessonAt(lesson.id.name))
        },
        onSettings = { navigator.go(Destination.Settings) },
        onPath = { navigator.go(Destination.Path(givensOf(game))) },
        onSpend = onSpendHint,
        modifier = modifier,
    )
}

/** The clues the game started from, as text, which is all the path screen needs. */
private fun givensOf(game: com.sendoku.app.game.GameState): String = buildString {
    for (cell in game.cells) {
        append(if (cell.isGiven) com.sendoku.engine.Digits.toChar(cell.digit) else '.')
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Sendoku.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.app_name), style = Sendoku.type.title, color = Sendoku.colors.muted)
    }
}

/**
 * Today, as a count of days since 1970.
 *
 * Local rather than UTC on purpose. Somebody in Auckland should get tomorrow's puzzle when
 * it is tomorrow where they are, not when it is tomorrow in London.
 */
internal fun todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()
