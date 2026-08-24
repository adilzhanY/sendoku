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
        SueDeCoq,
        AlsXyWing,
        DeathBlossom,
    ).sortedBy { it.id.cost }

    /**
     * Rules that work by assuming the puzzle has exactly one solution.
     *
     * They are perfectly sound on anything Sendoku ships, because the generator never
     * produces an ambiguous grid. They are not sound on an arbitrary grid: handed a puzzle
     * with two answers, they will happily rule out one of them and reach a single answer
     * that is not forced by logic at all.
     *
     * That matters wherever a grid arrives from outside the generator, and it is why the
     * distinction is part of the API rather than a comment.
     */
    public val assumesUniqueSolution: Set<TechniqueId> = setOf(
        TechniqueId.UNIQUE_RECTANGLE,
        TechniqueId.BUG_PLUS_ONE,
    )

    /**
     * The ladder with the uniqueness rules removed.
     *
     * Everything here follows from the grid alone. If this solves a puzzle, the puzzle is
     * solvable by pure deduction whether or not its answer happens to be unique.
     */
    public val logicOnly: List<Technique> = ladder.filter { it.id !in assumesUniqueSolution }

    /**
     * The rules that treat a group of cells as one thing.
     *
     * This is what the hardest grades are made of, and what mainstream apps stop short of.
     * Every one of them starts from an almost locked set: a group one digit away from
     * using up everything it holds. Naming the family here rather than listing it at each
     * call site means a new one joins the top of the ladder by being added once.
     */
    public val setLogic: Set<TechniqueId> = setOf(
        TechniqueId.ALS_XZ,
        TechniqueId.SUE_DE_COQ,
        TechniqueId.ALS_XY_WING,
        TechniqueId.DEATH_BLOSSOM,
    )

    public fun byId(id: TechniqueId): Technique? = ladder.firstOrNull { it.id == id }
}
