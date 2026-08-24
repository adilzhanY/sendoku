package com.sendoku.app.learn

import com.sendoku.app.data.LessonProgressDao
import com.sendoku.app.data.LessonProgressRow
import com.sendoku.app.data.TechniqueMasteryDao
import com.sendoku.app.data.TechniqueMasteryRow
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** How far through a lesson somebody is, in the shape the screens want it. */
public data class LessonProgress(val step: Int = 0, val finished: Boolean = false)

/** How well one technique is known. */
public data class Mastery(
    val attempts: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val mastered: Boolean = false,
    val lastPractisedAt: Long = 0,
)

/** Everything the course knows about a player, all of it from this device. */
public data class CourseProgress(
    val lessons: Map<LessonId, LessonProgress> = emptyMap(),
    val mastery: Map<TechniqueId, Mastery> = emptyMap(),
) {
    public val finishedCount: Int get() = lessons.values.count { it.finished }

    public fun isFinished(id: LessonId): Boolean = lessons[id]?.finished == true

    /** The first lesson not finished, which is what the home screen offers. */
    public fun next(): Lesson = Curriculum.lessons.firstOrNull { !isFinished(it.id) } ?: Curriculum.lessons.last()
}

/**
 * Where course progress is kept.
 *
 * Nothing here leaves the device and nothing here is a network call. The interface exists so
 * the screens can be tested without a database, not because a second implementation is coming.
 */
public interface LearningRepository {

    public fun progress(): Flow<CourseProgress>

    public suspend fun record(id: LessonId, step: Int, finished: Boolean, at: Long)

    public suspend fun recordPractice(technique: TechniqueId, correct: Boolean, at: Long)

    public suspend fun clear()
}

/**
 * The real one, over Room.
 *
 * Mastery is three correct in a row, and the number is written down here rather than being
 * folded into a condition somewhere. Three is short enough to reach in a sitting and long
 * enough that guessing twice does not do it. A wrong answer takes the streak back to zero;
 * being mastered once is not taken away again, because a course that can demote you is a
 * course people stop opening.
 */
public class RoomLearningRepository(private val lessons: LessonProgressDao, private val mastery: TechniqueMasteryDao) :
    LearningRepository {

    override fun progress(): Flow<CourseProgress> =
        combine(lessons.watchAll(), mastery.watchAll()) { lessonRows, masteryRows ->
            CourseProgress(
                lessons = lessonRows.mapNotNull { row ->
                    val id = runCatching { LessonId.valueOf(row.lessonId) }.getOrNull() ?: return@mapNotNull null
                    id to LessonProgress(row.step, row.finished)
                }.toMap(),
                mastery = masteryRows.mapNotNull { row ->
                    val id = runCatching { TechniqueId.valueOf(row.technique) }.getOrNull() ?: return@mapNotNull null
                    id to Mastery(row.attempts, row.correct, row.streak, row.mastered, row.lastPractisedAt)
                }.toMap(),
            )
        }

    override suspend fun record(id: LessonId, step: Int, finished: Boolean, at: Long) {
        val existing = lessons.of(id.name)
        lessons.save(
            LessonProgressRow(
                lessonId = id.name,
                // Never go backwards. Reopening a finished lesson to reread step two should not
                // report the player as two steps in.
                step = maxOf(step, existing?.step ?: 0),
                finished = finished || existing?.finished == true,
                lastSeenAt = at,
            ),
        )
    }

    override suspend fun recordPractice(technique: TechniqueId, correct: Boolean, at: Long) {
        val existing = mastery.of(technique.name)
        val streak = if (correct) (existing?.streak ?: 0) + 1 else 0
        mastery.save(
            TechniqueMasteryRow(
                technique = technique.name,
                attempts = (existing?.attempts ?: 0) + 1,
                correct = (existing?.correct ?: 0) + if (correct) 1 else 0,
                streak = streak,
                mastered = existing?.mastered == true || streak >= MASTERY_STREAK,
                lastPractisedAt = at,
            ),
        )
    }

    override suspend fun clear() {
        lessons.clear()
        mastery.clear()
    }

    public companion object {
        /** Three in a row. Short enough to reach in a sitting, long enough that luck will not. */
        public const val MASTERY_STREAK: Int = 3
    }
}

/** The lessons of the course with what the player has done to each, for the course map. */
public fun CourseProgress.map(): List<Pair<Lesson, LessonProgress>> =
    Curriculum.lessons.map { it to (lessons[it.id] ?: LessonProgress()) }

/** Progress through a stage, for the heading on the course map. */
public fun CourseProgress.of(stage: Stage): Pair<Int, Int> {
    val inStage = Curriculum.of(stage)
    return inStage.count { isFinished(it.id) } to inStage.size
}

/** Techniques the player has met a lesson for, which is what mixed practice may draw on. */
public fun CourseProgress.met(): List<TechniqueId> =
    Curriculum.lessons.filter { isFinished(it.id) }.flatMap { it.teaches }

/** True when the flow above has never been written to, so the course has not been opened. */
public val CourseProgress.isUntouched: Boolean get() = lessons.isEmpty() && mastery.isEmpty()
