package dev.puzzlecollection.numberlink

/**
 * The rules with no pruning: extend line 0 until it reaches its target, then line 1, and so on,
 * and accept only full boards. Too slow beyond 5x5; it exists to check NumberlinkSolver.
 */
object BruteForceCounter {
    fun count(size: Int, endpoints: List<Pair<Int, Int>>, limit: Int = 2): Int {
        val owner = IntArray(size * size) { -1 }
        endpoints.forEachIndexed { line, (a, b) -> owner[a] = line; owner[b] = line }
        var found = 0
        fun extend(line: Int, head: Int) {
            if (found >= limit) return
            if (line == endpoints.size) {
                if (owner.none { it == -1 }) found++
                return
            }
            val target = endpoints[line].second
            for (next in neighborsOf(head, size)) {
                if (next == target) {
                    extend(line + 1, endpoints.getOrNull(line + 1)?.first ?: -1)
                } else if (owner[next] == -1) {
                    owner[next] = line
                    extend(line, next)
                    owner[next] = -1
                }
            }
        }
        if (endpoints.isEmpty()) return 0
        extend(0, endpoints[0].first)
        return found
    }
}
