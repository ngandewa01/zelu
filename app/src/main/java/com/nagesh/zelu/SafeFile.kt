package com.nagesh.zelu

import java.io.File
import java.io.FileOutputStream

// Writes a file so a crash or power cut mid-write can never leave it half-written:
// the text goes to a temporary file first, is flushed to disk, and only then replaces the real file.
fun File.writeAtomic(text: String) {
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
