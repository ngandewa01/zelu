package com.nagesh.zelu

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

// Writes a CSV of transactions to the public Downloads/Zelu folder. Uses MediaStore so
// no storage permission is needed on Android 10+ (minSdk-24 devices below that return false).
object Exporter {

    fun exportCsv(context: Context, rows: List<Row>, filename: String): Boolean {
        val sb = StringBuilder("Date,Amount,Direction,Kind,Category,Payee,Account\n")
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        rows.sortedBy { it.date }.forEach { r ->
            sb.append(r.date).append(',')
                .append(r.amount).append(',')
                .append(r.direction).append(',')
                .append(r.kind).append(',')
                .append(esc(r.category)).append(',')
                .append(esc(r.counterparty ?: "")).append(',')
                .append(esc(r.account)).append('\n')
        }
        return writeToDownloads(context, filename, "text/csv", sb.toString())
    }

    private fun writeToDownloads(context: Context, filename: String, mime: String, content: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
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
        } catch (_: Exception) {
            false
        }
    }
}
