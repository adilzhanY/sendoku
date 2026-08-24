package com.sendoku.app.learn

import com.sendoku.engine.technique.TechniqueId

/**
 * What to practise next.
 *
 * There is no streak to keep, no daily target and no notification, so this cannot nag and is
 * not trying to. It answers one question when the player opens practice: of the techniques
 * they have met, which one is worth a go right now.
 *
 * The order is deliberate and short:
 *
 *  1. Anything met but never practised. A technique read about and never found is the one most
 *     likely to be forgotten.
 *  2. Anything practised but not yet mastered, hardest first, because that is where the
 *     difficulty actually is.
 *  3. Anything mastered a while ago, oldest first. This is the review queue, and the spacing
 *     is one week, which is long enough that a technique has gone quiet and short enough that
 *     it comes back before it is gone.
 *
 * A player who has mastered everything recently gets the oldest of them rather than nothing,
 * because "come back on Thursday" is exactly the kind of thing this app does not say.
 */
public object PracticePlan {

    /** How long a mastered technique rests before it is worth seeing again. */
    public const val REVIEW_AFTER_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

    /**
     * The technique to offer, or null when the player has met none yet.
     *
     * [met] is what their finished lessons have taught, so somebody who skipped ahead to the
     * chains is offered chains rather than singles.
     */
    public fun next(met: List<TechniqueId>, mastery: Map<TechniqueId, Mastery>, now: Long): TechniqueId? {
        if (met.isEmpty()) return null

        val unseen = met.firstOrNull { mastery[it] == null || mastery[it]?.attempts == 0 }
        if (unseen != null) return unseen

        val unmastered = met.filter { mastery[it]?.mastered != true }
        if (unmastered.isNotEmpty()) return unmastered.maxByOrNull { it.cost }

        val due = met.filter { (now - (mastery[it]?.lastPractisedAt ?: 0)) >= REVIEW_AFTER_MILLIS }
        if (due.isNotEmpty()) return due.minByOrNull { mastery[it]?.lastPractisedAt ?: 0 }

        // Everything mastered and everything recent. Offer the oldest anyway rather than
        // turning somebody away with a date.
        return met.minByOrNull { mastery[it]?.lastPractisedAt ?: 0 }
    }

    /**
     * A technique for mixed practice, drawn from everything met.
     *
     * Mixed is the harder exercise and the more honest one, because a real grid does not tell
     * you which pattern it is hiding. Weighted towards what is not mastered, so it is practice
     * rather than a parade of things already known.
     */
    public fun mixed(met: List<TechniqueId>, mastery: Map<TechniqueId, Mastery>, pick: Int): TechniqueId? {
        if (met.isEmpty()) return null
        val weighted = met.flatMap { technique ->
            val times = if (mastery[technique]?.mastered == true) 1 else 3
            List(times) { technique }
        }
        return weighted[Math.floorMod(pick, weighted.size)]
    }

    /**
     * Whether practising [technique] is jumping ahead of the course.
     *
     * Allowed, with a word of warning rather than a lock. Somebody curious about chains on
     * their second day is a player worth keeping, and a locked door teaches nothing.
     */
    public fun isAhead(technique: TechniqueId, met: List<TechniqueId>): Boolean = technique !in met
}
