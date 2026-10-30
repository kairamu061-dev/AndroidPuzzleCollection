package dev.puzzlecollection.sudoku

import android.util.AtomicFile
import java.io.File
import java.io.IOException

/**
 * Blocking file I/O: call off the main thread, one call at a time.
 * [read] during [write] makes AtomicFile restore its backup over the file being written.
 */
class SaveRepository(filesDir: File) {
    private val file = AtomicFile(File(File(filesDir, "sudoku"), "save.txt"))

    fun read(): SavedGame? = try {
        SaveCodec.decode(file.readFully().decodeToString())
    } catch (_: IOException) {
        null
    }

    fun write(game: SavedGame) {
        file.baseFile.parentFile?.mkdirs()
        val stream = try {
            file.startWrite()
        } catch (_: IOException) {
            return
        }
        try {
            stream.write(SaveCodec.encode(game).encodeToByteArray())
            file.finishWrite(stream)
        } catch (_: IOException) {
            file.failWrite(stream)
        }
    }

    fun delete() {
        file.delete()
    }
}
