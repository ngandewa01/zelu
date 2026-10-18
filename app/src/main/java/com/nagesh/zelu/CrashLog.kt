package com.nagesh.zelu

import android.app.Application
import android.content.Context
import java.io.File

// Keeps the details of the last crash on the phone (nothing is sent anywhere), so "Report a problem" can attach them
// when you choose to.
class ZeluApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { CrashLog.save(this, e) } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }
    }
}

object CrashLog {
    private fun file(c: Context) = File(c.filesDir, "last_crash.log")

    fun save(c: Context, e: Throwable) {
        val sb = StringBuilder()
        sb.append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())).append('\n')
        var cur: Throwable? = e
        var depth = 0
        while (cur != null && depth < 3) {
            sb.append(cur.javaClass.name).append(": ").append(Redact.message(cur.message ?: "")).append('\n')
            cur.stackTrace.take(12).forEach { sb.append("  at ").append(it).append('\n') }
            cur = cur.cause; depth++
        }
        file(c).writeAtomic(sb.toString())
    }

    fun last(c: Context): String? = file(c).takeIf { it.exists() }?.readText()
    fun clear(c: Context) { file(c).delete() }
}
