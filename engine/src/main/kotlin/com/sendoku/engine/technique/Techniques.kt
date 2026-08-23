package com.sendoku.engine.technique

/** The full set of rules Sendoku knows, and the order it tries them in. */
public object Techniques {

    /**
     * Every technique, cheapest first.
     *
     * Sorting by cost rather than by hand is what keeps the solver honest: a technique
     * cannot quietly jump the queue by being declared earlier, and retuning a cost
     * reorders the search with no other change. Ties keep declaration order, which makes
     * the ladder stable between runs.
     */
    public val ladder: List<Technique> = listOf(
        NakedSingle,
        HiddenSingle,
        PointingCandidates,
        ClaimingCandidates,
        NakedPair,
        HiddenPair,
        NakedTriple,
        HiddenTriple,
        NakedQuad,
        HiddenQuad,
        XWing,
        Swordfish,
        Jellyfish,
        XYWing,
        XYZWing,
        WWing,
        SimpleColouring,
        MultiColouring,
        UniqueRectangle,
        BugPlusOne,
        RemotePairs,
        XChain,
        XYChain,
        AlsXz,
    ).sortedBy { it.id.cost }

    public fun byId(id: TechniqueId): Technique? = ladder.firstOrNull { it.id == id }
}
