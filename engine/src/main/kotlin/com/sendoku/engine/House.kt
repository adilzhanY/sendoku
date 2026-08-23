package com.sendoku.engine

/** The three kinds of region that must hold every digit exactly once. */
public enum class HouseKind {
    ROW,
    COLUMN,
    BOX,
}

/**
 * One region of the grid, named rather than listed.
 *
 * A house is a pointer, not a container. The cells belong to the grid shape, so a
 * [CandidateGrid] resolves them with [CandidateGrid.cellsOf]. That keeps a [House]
 * cheap to compare and safe to put inside a hint.
 */
public data class House(val kind: HouseKind, val index: Int) {

    /** Reads as `row 4`, counting from one the way a player would. */
    override fun toString(): String = "${kind.name.lowercase()} ${index + 1}"
}

/**
 * The geometry of one grid shape: which cells sit in which house, and which cells see
 * which others.
 *
 * None of this depends on the digits, only on [Dimensions], so it is built once per
 * shape and shared by every grid of that shape. Rebuilding it per puzzle would be pure
 * waste when a batch run rates thousands of grids.
 */
internal class Topology private constructor(val dims: Dimensions) {

    private val size = dims.size

    val rowCells: Array<IntArray> = Array(size) { row -> IntArray(size) { col -> row * size + col } }

    val colCells: Array<IntArray> = Array(size) { col -> IntArray(size) { row -> row * size + col } }

    val boxCells: Array<IntArray> = run {
        val buckets = Array(size) { ArrayList<Int>(size) }
        for (index in 0 until dims.cellCount) {
            buckets[dims.boxOf(index / size, index % size)].add(index)
        }
        Array(size) { buckets[it].toIntArray() }
    }

    /** Rows first, then columns, then boxes. Techniques scan in this order. */
    val houses: List<House> = HouseKind.entries.flatMap { kind ->
        List(size) { index -> House(kind, index) }
    }

    val housesOfCell: Array<List<House>> = Array(dims.cellCount) { index ->
        listOf(
            House(HouseKind.ROW, index / size),
            House(HouseKind.COLUMN, index % size),
            House(HouseKind.BOX, dims.boxOf(index / size, index % size)),
        )
    }

    val peers: Array<IntArray> = Array(dims.cellCount) { index ->
        val seen = LinkedHashSet<Int>()
        for (house in housesOfCell[index]) seen.addAll(cellsOf(house).asIterable())
        seen.remove(index)
        seen.toIntArray()
    }

    fun cellsOf(house: House): IntArray = when (house.kind) {
        HouseKind.ROW -> rowCells[house.index]
        HouseKind.COLUMN -> colCells[house.index]
        HouseKind.BOX -> boxCells[house.index]
    }

    companion object {
        private val cache = java.util.concurrent.ConcurrentHashMap<Dimensions, Topology>()

        fun of(dims: Dimensions): Topology = cache.getOrPut(dims) { Topology(dims) }
    }
}
