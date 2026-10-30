package dev.puzzlecollection.sudoku

fun digits(text: String): List<Int> = text.filter { it.isDigit() }.map { it - '0' }

val SAMPLE_GIVENS = digits(
    """
    530 070 000
    600 195 000
    098 000 060
    800 060 003
    400 803 001
    700 020 006
    060 000 280
    000 419 005
    000 080 079
    """,
)

val SAMPLE_SOLUTION = digits(
    """
    534 678 912
    672 195 348
    198 342 567
    859 761 423
    426 853 791
    713 924 856
    961 537 284
    287 419 635
    345 286 179
    """,
)

val SAMPLE_PUZZLE = Puzzle(SAMPLE_GIVENS, SAMPLE_SOLUTION, Difficulty.EASY)

const val R0C2 = 2   // empty, solution 4
const val R0C3 = 3   // empty, solution 6
const val R1C1 = 10  // empty, solution 7, same box as R0C2
const val R4C2 = 38  // empty, same column as R0C2
const val R8C8 = 80  // given 9
const val FAR_CELL = 62 // empty, shares no unit with R0C2
