package com.koolicream.zelu

import android.content.BroadcastReceiver

// A receiver runs on the main thread, where reading every payment or encrypting the weekly backup could freeze the
// phone. This runs the work on its own thread and keeps the app alive until it is done.
fun BroadcastReceiver.inBackground(work: () -> Unit) {
    val where = javaClass.simpleName
    val pending = goAsync()
    Thread {
        try {
            work()
        } catch (e: Exception) {
            // Never crash on a message, an alarm or a notification button, but leave a trace.
            CrashLog.warn(where, e)
        } finally {
            pending.finish()
        }
    }.start()
}
