package com.example.walnutapp

import android.app.Application
import android.content.Intent
import android.os.Process
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class MainApplication : Application(), ReactApplication {

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages.apply {
                    // Native SMS reader and background receiver queue.
                    add(SmsPackage())
                }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = false
            override val isHermesEnabled: Boolean = true
        }

    override val reactHost: ReactHost by lazy {
        getDefaultReactHost(applicationContext, reactNativeHost)
    }

    override fun onCreate() {
        // Catch any startup crash (including native library load failures)
        // and show it on a plain screen instead of a silent "keeps stopping".
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val intent = Intent(this, CrashActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("error", sw.toString())
                startActivity(intent)
            } catch (_: Throwable) {
                // ignore; nothing more we can do
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
        super.onCreate()
        SoLoader.init(this, false)
    }
}
