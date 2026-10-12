package com.example.walnutapp

import android.content.Context
import android.net.Uri
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import org.json.JSONArray

// Exposed to JavaScript as NativeModules.SmsReader.
class SmsModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SmsReader"

    // Reads the most recent inbox SMS (requires READ_SMS).
    @ReactMethod
    fun readInbox(limit: Double, promise: Promise) {
        try {
            val result = Arguments.createArray()
            reactApplicationContext.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC",
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")
                var count = 0
                val max = limit.toInt()
                while (cursor.moveToNext() && count < max) {
                    count++
                    val map = Arguments.createMap()
                    map.putString("address", cursor.getString(addressIdx) ?: "")
                    map.putString("body", cursor.getString(bodyIdx) ?: "")
                    map.putDouble("date", cursor.getLong(dateIdx).toDouble())
                    result.pushMap(map)
                }
            }
            promise.resolve(result)
        } catch (e: SecurityException) {
            promise.reject("PERMISSION_DENIED", "READ_SMS permission not granted", e)
        } catch (e: Exception) {
            promise.reject("READ_FAILED", e.message, e)
        }
    }

    // Returns SMS captured by SmsReceiver while the app was closed, then clears the queue.
    @ReactMethod
    fun drainQueue(promise: Promise) {
        try {
            val prefs = reactApplicationContext.getSharedPreferences(
                SmsReceiver.QUEUE_PREFS,
                Context.MODE_PRIVATE,
            )
            var items = "[]"
            synchronized(SmsReceiver.LOCK) {
                items = prefs.getString("items", "[]") ?: "[]"
                prefs.edit().putString("items", "[]").apply()
            }

            val result = Arguments.createArray()
            val json = JSONArray(items)
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val map = Arguments.createMap()
                map.putString("address", obj.optString("address"))
                map.putString("body", obj.optString("body"))
                map.putDouble("date", obj.optDouble("date"))
                result.pushMap(map)
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("QUEUE_FAILED", e.message, e)
        }
    }
}
