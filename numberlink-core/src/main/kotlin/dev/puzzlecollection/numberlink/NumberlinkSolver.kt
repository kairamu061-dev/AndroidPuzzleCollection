package dev.puzzlecollection.numberlink

/**
 * Counts solutions under the rules in docs/numberlink/spec.md: every pair joined by one
 * orthogonal line, no crossing, every cell covered.
 *
 * Each solution is reached by exactly one branch: a node extends a single chosen line, and a
 * solution fixes that line's next cell. Pruning must only cut nodes that cannot complete;
 * an over-eager cut silently lets a two-solution puzzle through, which is why
 * BruteForceCounter exists in the tests. Adjacent endpoints are not joined up front: the line
 * may still take the long way round.
 */
object NumberlinkSolver {
    /** Null when [nodeBudget] runs out: the count is then unknown, never "unique". */
    fun countSolutions(
        size: Int,
        endpoints: List<Pair<Int, Int>>,
        limit: Int = 2,
        nodeBudget: Int = DEFAULT_BUDGET,
        onCheck: () -> Unit = {},
    ): Int? = findSolutions(size, endpoints, limit, nodeBudget, onCheck)?.size

    /**
     * Up to [limit] solutions, each as its set of joined cell pairs (see [edgeKey]).
     * Null when [nodeBudget] runs out.
     */
    fun findSolutions(
        size: Int,
        endpoints: List<Pair<Int, Int>>,
        limit: Int = 2,
        nodeBudget: Int = DEFAULT_BUDGET,
        onCheck: () -> Unit = {},
    ): List<Set<Long>>? {
        val search = Search(size, endpoints, limit, nodeBudget, onCheck)
        return if (search.run()) search.solutions else null
    }

    /** One joined pair of neighbouring cells, independent of direction. */
    fun edgeKey(a: Int, b: Int): Long = minOf(a, b).toLong() shl 32 or maxOf(a, b).toLong()

    const val DEFAULT_BUDGET = 2_000_000

    private class Search(
        private val size: Int,
        endpoints: List<Pair<Int, Int>>,
        private val limit: Int,
        private val budget: Int,
        private val onCheck: () -> Unit,
    ) {
        private val cellCount = size * size
        private val owner = IntArray(cellCount) { EMPTY }
        private val heads = IntArray(endpoints.size) { endpoints[it].first }
        private val targets = IntArray(endpoints.size) { endpoints[it].second }
        private val done = BooleanArray(endpoints.size)
        private val neighbors = Array(cellCount) { neighborsOf(it, size) }
        private val component = IntArray(cellCount)
        private val queue = IntArray(cellCount)
        val solutions = mutableListOf<Set<Long>>()
        private val found get() = solutions.size
        private val previous = IntArray(cellCount) { -1 }
        private var nodes = 0

        init {
            endpoints.forEachIndexed { line, (a, b) ->
                owner[a] = line
                owner[b] = line
            }
        }

        /** False when the budget ran out. */
        fun run(): Boolean = try {
            descend()
            true
        } catch (_: BudgetExceeded) {
            false
        }

        private fun descend() {
            if (++nodes > budget) throw BudgetExceeded
            if (nodes and 1023 == 0) onCheck()
            if (!feasible()) return
            var best = -1
            var bestCount = Int.MAX_VALUE
            for (line in heads.indices) {
                if (done[line]) continue
                val count = movesOf(line).size
                if (count < bestCount) {
                    best = line
                    bestCount = count
                }
            }
            if (best < 0) {
                if (owner.none { it == EMPTY }) solutions += currentEdges()
                return
            }
            val head = heads[best]
            for (next in movesOf(best)) {
                previous[next] = head
                if (next == targets[best]) {
                    done[best] = true
                    heads[best] = next
                    descend()
                    heads[best] = head
                    done[best] = false
                } else {
                    owner[next] = best
                    heads[best] = next
                    descend()
                    heads[best] = head
                    owner[next] = EMPTY
                }
                previous[next] = -1
                if (found >= limit) return
            }
        }

        private fun currentEdges(): Set<Long> = buildSet {
            for (cell in 0 until cellCount) {
                if (previous[cell] >= 0) add(edgeKey(cell, previous[cell]))
            }
        }

        private fun movesOf(line: Int): List<Int> =
            neighbors[heads[line]].filter { owner[it] == EMPTY || it == targets[line] }

        /** Necessary conditions only; see the class comment. */
        private fun feasible(): Boolean {
            // An empty cell needs two ways in or out: empty cells, live heads, or live targets.
            for (cell in 0 until cellCount) {
                if (owner[cell] != EMPTY) continue
                var open = 0
                for (n in neighbors[cell]) {
                    if (owner[n] == EMPTY || isLiveEnd(n)) open++
                }
                if (open < 2) return false
            }
            // Label the empty regions.
            component.fill(-1)
            var regions = 0
            for (start in 0 until cellCount) {
                if (owner[start] != EMPTY || component[start] >= 0) continue
                var read = 0
                var write = 0
                queue[write++] = start
                component[start] = regions
                while (read < write) {
                    val cell = queue[read++]
                    for (n in neighbors[cell]) {
                        if (owner[n] == EMPTY && component[n] < 0) {
                            component[n] = regions
                            queue[write++] = n
                        }
                    }
                }
                regions++
            }
            // Every live line must reach its target, and every region must be usable by some line.
            val usable = BooleanArray(regions)
            for (line in heads.indices) {
                if (done[line]) continue
                val head = heads[line]
                val target = targets[line]
                if (target in neighbors[head]) {
                    markShared(head, target, usable)
                    continue
                }
                if (!markShared(head, target, usable)) return false
            }
            return usable.all { it }
        }

        /** Marks regions touching both cells; true if there was at least one. */
        private fun markShared(a: Int, b: Int, usable: BooleanArray): Boolean {
            var shared = false
            for (na in neighbors[a]) {
                if (owner[na] != EMPTY) continue
                val region = component[na]
                if (neighbors[b].any { owner[it] == EMPTY && component[it] == region }) {
                    usable[region] = true
                    shared = true
                }
            }
            return shared
        }

        private fun isLiveEnd(cell: Int): Boolean {
            val line = owner[cell]
            return line >= 0 && !done[line] && (heads[line] == cell || targets[line] == cell)
        }
    }

    private object BudgetExceeded : RuntimeException() {
        private fun readResolve(): Any = BudgetExceeded
        override fun fillInStackTrace(): Throwable = this
    }

    private const val EMPTY = -1
}

internal fun neighborsOf(cell: Int, size: Int): IntArray {
    val row = cell / size
    val column = cell % size
    return buildList {
        if (row > 0) add(cell - size)
        if (row < size - 1) add(cell + size)
        if (column > 0) add(cell - 1)
        if (column < size - 1) add(cell + 1)
    }.toIntArray()
}
