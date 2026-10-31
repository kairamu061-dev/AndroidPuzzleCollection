package dev.puzzlecollection.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaveFileTest {
    @get:Rule val folder = TemporaryFolder()
    private val file by lazy { SaveFile(folder.root, "game/save.txt") }

    @Test fun `I-01 write then read, overwrite`() {
        file.write("one")
        assertEquals("one", file.read())
        file.write("two")
        assertEquals("two", file.read())
    }

    @Test fun `I-02 a missing file reads as null`() {
        assertNull(file.read())
    }

    @Test fun `I-03 delete removes the file`() {
        file.write("one")
        file.delete()
        assertNull(file.read())
    }
}
