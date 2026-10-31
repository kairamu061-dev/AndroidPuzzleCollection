package dev.puzzlecollection.nonogram

/** [id] is stored in saves and progress: renaming one loses the player's record of it. */
data class NonogramPicture(val id: String, val name: String, val puzzle: NonogramPuzzle)

/** Format: docs/nonogram/pictures/design.md. */
object NonogramPictures {
    /** Reads a bundled resource: touch this off the main thread. */
    val all: List<NonogramPicture> by lazy {
        val stream = checkNotNull(javaClass.getResourceAsStream("pictures.tsv"))
        parse(stream.use { it.readBytes().decodeToString() })
    }

    fun parse(text: String): List<NonogramPicture> = text.lines().filter { it.isNotEmpty() }.map { line ->
        val (id, name, bits) = line.split("\t")
        NonogramPicture(id, name, NonogramPuzzle(PICTURE_SIZE, bits.map { it == '1' }))
    }
}

/** The list order is the play order. */
fun nextPicture(pictures: List<NonogramPicture>, completed: Set<String>): NonogramPicture? =
    pictures.firstOrNull { it.id !in completed }

/** Unknown ids are kept: a picture removed in one version may return in a later one. */
object NonogramProgress {
    fun encode(completed: Set<String>): String = completed.joinToString("\n", postfix = "\n")

    fun decode(text: String): Set<String> = text.lines().filter { it.isNotEmpty() }.toSet()
}
