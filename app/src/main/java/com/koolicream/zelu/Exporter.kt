package com.koolicream.zelu

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentUris
import androidx.core.content.ContextCompat
import java.io.File

// Writes a CSV of transactions (or a backup) to the public Downloads/Zelu folder. Android 10+ uses MediaStore, which
// needs no permission; Android 7-9 writes the file directly, which needs storage access (asked for when you save).
object Exporter {

    /** True on Android 7-9 while storage access has not been given; ask for it before saving. */
    fun needsPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun legacyDir(): File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Zelu")

    fun exportCsv(context: Context, rows: List<Row>, filename: String): Boolean {
        val sb = StringBuilder("Date,Amount,Direction,Kind,Category,Payee,Account\n")
        rows.sortedBy { it.date }.forEach { r ->
            sb.append(r.date).append(',')
                .append(r.amount).append(',')
                .append(r.direction.key).append(',')
                .append(r.kind.key).append(',')
                .append(esc(r.category)).append(',')
                .append(esc(r.counterparty ?: "")).append(',')
                .append(esc(r.account)).append('\n')
        }
        return writeToDownloads(context, filename, "text/csv", sb.toString())
    }

    // A payee comes from the text of an SMS, so a spoofed message could carry "=HYPERLINK(...)"; a spreadsheet would run
    // a cell starting with = + - @ (or a tab / return) as a formula. A leading apostrophe keeps it plain text.
    internal fun esc(s: String): String {
        val safe = if (s.isNotEmpty() && s[0] in "=+-@\t\r") "'$s" else s
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    fun exportText(context: Context, filename: String, mime: String, content: String): Boolean =
        writeToDownloads(context, filename, mime, content)

    // Backups would pile up in Downloads/Zelu week after week; only the newest few are kept. Android lets an app remove
    // only files it saved itself, so backups from before a reinstall stay until you delete them.
    fun pruneBackups(context: Context, keep: Int = 4) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (needsPermission(context)) return
            try {
                legacyDir().listFiles { f -> f.name.startsWith("zelu-backup-") }?.sortedByDescending { it.lastModified() }
                    ?.drop(keep)?.forEach { it.delete() }
            } catch (e: Exception) { CrashLog.warn("Exporter.pruneBackups", e) }
            return
        }
        try {
            val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val sel = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val args = arrayOf(Environment.DIRECTORY_DOWNLOADS + "/Zelu%", "zelu-backup-%")
            val ids = ArrayList<Long>()
            context.contentResolver.query(uri, arrayOf(MediaStore.Downloads._ID), sel, args, "${MediaStore.Downloads.DATE_ADDED} DESC")?.use { c ->
                val col = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                while (c.moveToNext()) ids.add(c.getLong(col))
            }
            for (id in ids.drop(keep)) {
                try { context.contentResolver.delete(ContentUris.withAppendedId(uri, id), null, null) } catch (e: Exception) { CrashLog.warn("Exporter.pruneBackups", e) }
            }
        } catch (e: Exception) { CrashLog.warn("Exporter.pruneBackups", e) }
    }

    private fun writeToDownloads(context: Context, filename: String, mime: String, content: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return writeLegacy(context, filename, mime, content)
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Zelu")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            true
        } catch (e: Exception) {
            CrashLog.warn("Exporter.writeToDownloads", e)
            false
        }
    }

    private fun writeLegacy(context: Context, filename: String, mime: String, content: String): Boolean {
        if (needsPermission(context)) return false
        return try {
            val dir = legacyDir().apply { mkdirs() }
            val f = File(dir, filename)
            f.writeAtomic(content)
            // Shows the new file in the Files app and to a computer over USB straight away.
            MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), arrayOf(mime), null)
            true
        } catch (e: Exception) {
            CrashLog.warn("Exporter.writeLegacy", e)
            false
        }
    }
}
