package com.sendoku.engine

/**
 * The eight difficulty bands a puzzle can ship under.
 *
 * The bands are cut so that each one names a real jump in what a player has to know, not
 * an arbitrary slice of a number line. Gentle needs nothing but singles. Steady adds
 * locked candidates. Tricky brings the first pattern that spans the whole grid. Severe
 * needs wings and colouring. Diabolical needs chains. Beyond needs a rule that treats a
 * group of cells as one, which is further than most apps go at all.
 *
 * The last two go further still. Insane needs several of those groups chained together, so
 * that a group being full forces the next one. Nightmare needs a fork: a cell taken both
 * ways, with each branch followed until they agree or one of them dies. Both are past the
 * point where a puzzle can be held in the head, and the app says so rather than pretending
 * otherwise.
 *
 * [maxRating] is exclusive, so a rating sits in the first band it falls under. Each bound
 * is placed on the cost of the cheapest technique in the band above, which is what keeps
 * the repetition bonus from ever carrying a puzzle over a boundary.
 */
public enum class Grade(public val displayName: String, public val maxRating: Double) {
    GENTLE("Gentle", 2.0),
    STEADY("Steady", 3.2),
    TRICKY("Tricky", 4.2),
    SEVERE("Severe", 6.0),
    DIABOLICAL("Diabolical", 7.2),
    BEYOND("Beyond", 8.4),
    INSANE("Insane", 9.0),
    NIGHTMARE("Nightmare", Double.POSITIVE_INFINITY),
    ;

    /**
     * True for the bands that need a rule from the deep end of the ladder.
     *
     * From Beyond upwards a puzzle cannot be finished by spotting a shape. It needs a rule
     * that treats a group of cells as one thing, or a cell taken both ways on paper, and
     * the shipped catalog proves that of every puzzle it files here. The app marks these
     * levels differently because somebody who wanders into one without knowing that will
     * conclude the puzzle is broken rather than that it is hard.
     */
    public val isAdvanced: Boolean get() = ordinal >= BEYOND.ordinal

    public companion object {
        /** The band [rating] falls in. A rating of zero means nothing was needed. */
        public fun of(rating: Double): Grade = entries.first { rating < it.maxRating }
    }
}
