package com.example.walnutapp

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

// A plain Android screen (no React Native) that shows a startup crash
// message. Used so native crashes that happen before JS loads are visible
// on the phone instead of only appearing as "Zelu keeps stopping".
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.setTextIsSelectable(true)
        tv.textSize = 12f
        tv.setPadding(24, 80, 24, 24)
        tv.text = intent.getStringExtra("error") ?: "No error text captured."
        val sv = ScrollView(this)
        sv.addView(tv)
        setContentView(sv)
    }
}
