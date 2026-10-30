package dev.puzzlecollection.sudoku

/**
 * Rates a puzzle by the techniques it needs. Definitions: docs/sudoku/engine/spec.md.
 * Expects a puzzle with a unique solution; candidates can then never run out.
 */
object Grader {
    fun grade(givens: List<Int>): Difficulty {
        val board = Candidates(givens)
        var usedIntermediate = false
        while (true) {
            board.fillSingles()
            if (board.isComplete) {
                return if (usedIntermediate) Difficulty.MEDIUM else Difficulty.EASY
            }
            if (!board.eliminateByIntermediateTechniques()) return Difficulty.HARD
            usedIntermediate = true
        }
    }

    private class Candidates(givens: List<Int>) {
        private val values = IntArray(CELL_COUNT)
        private val masks = IntArray(CELL_COUNT) { ALL_DIGITS }

        val isComplete: Boolean get() = values.none { it == BLANK }

        init {
            for (cell in 0 until CELL_COUNT) {
                if (givens[cell] != BLANK) place(cell, givens[cell])
            }
        }

        private fun place(cell: Int, digit: Int) {
            values[cell] = digit
            masks[cell] = 0
            val keep = digitBit(digit).inv()
            for (peer in Geometry.peers[cell]) masks[peer] = masks[peer] and keep
        }

        fun fillSingles() {
            var progressed = true
            while (progressed) {
                progressed = fillNakedSingles() || fillHiddenSingles()
            }
        }

        private fun fillNakedSingles(): Boolean {
            var progressed = false
            for (cell in 0 until CELL_COUNT) {
                if (Integer.bitCount(masks[cell]) == 1) {
                    place(cell, Integer.numberOfTrailingZeros(masks[cell]))
                    progressed = true
                }
            }
            return progressed
        }

        private fun fillHiddenSingles(): Boolean {
            var progressed = false
            for (unit in Geometry.units) {
                for (digit in DIGITS) {
                    val bit = digitBit(digit)
                    val cell = unit.singleOrNull { masks[it] and bit != 0 } ?: continue
                    place(cell, digit)
                    progressed = true
                }
            }
            return progressed
        }

        fun eliminateByIntermediateTechniques(): Boolean =
            lockedCandidates() || nakedPairs() || hiddenPairs()

        /** Pointing and claiming: a digit confined to the overlap of two units. */
        private fun lockedCandidates(): Boolean {
            var progressed = false
            for (unit in Geometry.units) {
                for (digit in DIGITS) {
                    val bit = digitBit(digit)
                    val cells = unit.filter { masks[it] and bit != 0 }
                    if (cells.size < 2) continue
                    for (other in Geometry.unitsOfCell[cells.first()]) {
                        if (other === unit || !cells.all { it in other }) continue
                        for (cell in other) {
                            if (cell !in unit && masks[cell] and bit != 0) {
                                masks[cell] = masks[cell] and bit.inv()
                                progressed = true
                            }
                        }
                    }
                }
            }
            return progressed
        }

        private fun nakedPairs(): Boolean {
            var progressed = false
            for (unit in Geometry.units) {
                val pairs = unit.filter { Integer.bitCount(masks[it]) == 2 }
                    .groupBy { masks[it] }.filterValues { it.size == 2 }
                for ((pairMask, pairCells) in pairs) {
                    for (cell in unit) {
                        if (cell !in pairCells && masks[cell] and pairMask != 0) {
                            masks[cell] = masks[cell] and pairMask.inv()
                            progressed = true
                        }
                    }
                }
            }
            return progressed
        }

        private fun hiddenPairs(): Boolean {
            var progressed = false
            for (unit in Geometry.units) {
                val placements = DIGITS.associateWith { digit ->
                    unit.filter { masks[it] and digitBit(digit) != 0 }
                }.filterValues { it.size == 2 }
                for ((cells, digits) in placements.keys.groupBy { placements.getValue(it) }) {
                    if (digits.size != 2) continue
                    val keep = digitBit(digits[0]) or digitBit(digits[1])
                    for (cell in cells) {
                        if (masks[cell] and keep.inv() != 0) {
                            masks[cell] = masks[cell] and keep
                            progressed = true
                        }
                    }
                }
            }
            return progressed
        }
    }

    private val ALL_DIGITS = DIGITS.fold(0) { mask, digit -> mask or digitBit(digit) }
}
