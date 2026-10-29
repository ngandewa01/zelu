package com.koolicream.zelu

import android.app.Application
import android.content.Context
import java.io.File

// Keeps the details of the last crash on the phone (nothing is sent anywhere). On the next launch Zelu offers to
// send them by email, and "Report a problem" can attach them; the user sees the email and sends it themselves.
class ZeluApplication : Application() {
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
    private fun newFlag(c: Context) = File(c.filesDir, "last_crash.new")

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
        newFlag(c).writeText("1")
    }

    /**
     * A failure Zelu got past (a message it couldn't store, an alarm whose work failed): written to the system log so it
     * shows up while debugging, instead of vanishing. The message is redacted like a crash report, since it may quote an SMS.
     */
    fun warn(where: String, e: Throwable) {
        try {
            val top = e.stackTrace.take(4).joinToString(" < ")
            android.util.Log.w("Zelu", where + ": " + e.javaClass.name + ": " + Redact.message(e.message ?: "") + " at " + top)
        } catch (_: Throwable) {}
    }

    /** True once after a crash, so the next launch can offer to send a report. */
    fun isNew(c: Context): Boolean = newFlag(c).exists()
    fun markSeen(c: Context) { newFlag(c).delete() }

    fun last(c: Context): String? = file(c).takeIf { it.exists() }?.readText()
    fun clear(c: Context) { file(c).delete(); markSeen(c) }
}
