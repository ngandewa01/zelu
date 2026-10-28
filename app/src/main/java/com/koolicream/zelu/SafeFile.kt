package com.koolicream.zelu

import java.io.File
import java.io.FileOutputStream

// Writes a file so a crash or power cut mid-write can never leave it half-written:
// the text goes to a temporary file first, is flushed to disk, and only then replaces the real file.
object SafeFileStats { @Volatile var writes = 0L }

fun File.writeAtomic(text: String) {
    SafeFileStats.writes++
    val tmp = File(parentFile, "$name.tmp")
    FileOutputStream(tmp).use { out ->
        out.write(text.toByteArray(Charsets.UTF_8))
        out.flush()
        try { out.fd.sync() } catch (_: Exception) {}
    }
    if (!tmp.renameTo(this)) {
        // Some file systems refuse to rename over an existing file; remove it and try once more.
        delete()
        if (!tmp.renameTo(this)) { tmp.copyTo(this, overwrite = true); tmp.delete() }
    }
}

// Saves that keep the previous version as "<name>.bak", and reads that fall back to it. A file that can't be read
// (a failing card, a crash in an older version) is kept aside as "<name>.corrupt" and the previous version is read
// instead, so the next save can't replace everything in it with an almost empty list.
fun File.writeKeepingBackup(text: String) {
    if (exists() && length() > 2) try { copyTo(File(parentFile, "$name.bak"), overwrite = true) } catch (_: Exception) {}
    writeAtomic(text)
}

fun <T> File.readWithBackup(parse: (String) -> T): T = try { parse(readText()) } catch (e: Exception) {
    try { if (exists()) copyTo(File(parentFile, "$name.corrupt"), overwrite = true) } catch (_: Exception) {}
    val bak = File(parentFile, "$name.bak")
    if (!bak.exists()) throw e
    parse(bak.readText())
}
