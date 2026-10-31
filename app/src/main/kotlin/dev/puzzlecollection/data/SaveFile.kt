package dev.puzzlecollection.data

import android.util.AtomicFile
import java.io.File
import java.io.IOException

/**
 * Blocking file I/O: call off the main thread, one call at a time.
 * [read] during [write] makes AtomicFile restore its backup over the file being written.
 */
class SaveFile(filesDir: File, relativePath: String) {
    private val file = AtomicFile(File(filesDir, relativePath))

    fun read(): String? = try {
        file.readFully().decodeToString()
    } catch (_: IOException) {
        null
    }

    fun write(text: String) {
        file.baseFile.parentFile?.mkdirs()
        val stream = try {
            file.startWrite()
        } catch (_: IOException) {
            return
        }
        try {
            stream.write(text.encodeToByteArray())
            file.finishWrite(stream)
        } catch (_: IOException) {
            file.failWrite(stream)
        }
    }

    fun delete() {
        file.delete()
    }
}
