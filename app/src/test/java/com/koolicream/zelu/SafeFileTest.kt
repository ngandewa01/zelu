package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

// A damaged file falls back to the version kept from the previous save (see SafeFile.kt).
class SafeFileTest {
    private fun dir(): File = Files.createTempDirectory("zelu").toFile()

    @Test
    fun aDamagedFileIsReadFromThePreviousSave() {
        val f = File(dir(), "overrides.json")
        f.writeKeepingBackup("""{"a":"expense|Food"}""")
        f.writeKeepingBackup("""{"a":"expense|Food","b":"expense|Rent"}""")
        assertEquals("""{"a":"expense|Food"}""", File(f.parentFile, "overrides.json.bak").readText())
        f.writeText("""{"a":"exp""")                               // cut short
        val read = f.readWithBackup { text -> require(text.trim().endsWith("}")); text }
        assertEquals("""{"a":"expense|Food"}""", read)
        assertTrue("the damaged file is kept aside", File(f.parentFile, "overrides.json.corrupt").exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun withNoPreviousSaveTheFailureIsReported() {
        val f = File(dir(), "plan.json")
        f.writeText("{")
        f.readWithBackup { text -> require(text.endsWith("}")); text }
    }
}
