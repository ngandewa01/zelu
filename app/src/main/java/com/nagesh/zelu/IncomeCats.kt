package com.nagesh.zelu

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import java.io.File

// Categories for money coming in (salary, interest...). The built-in ones stay; you can add your own and delete those.
object IncomeCats {
    val defaults = listOf("Salary", "Interest", "Dividend", "Reimbursement", "Rewards", "Business", "Gift", "Rent received")
    var custom by mutableStateOf<List<String>>(emptyList())
    val all: List<String> get() = defaults + custom

    private val shades = listOf(0xFF4F8A6C, 0xFF3E9C86, 0xFF6E8F72, 0xFF5E9A8C, 0xFF7E9F5E, 0xFF4E8AA0, 0xFF8AA05E, 0xFF5A9A7A)
    fun color(name: String): Color? {
        val i = all.indexOf(name)
        return if (i < 0) null else Color(shades[i % shades.size])
    }

    private fun file(c: Context) = File(c.filesDir, "income_categories.json")

    fun load(c: Context) {
        custom = try {
            val f = file(c)
            if (!f.exists()) emptyList() else JSONArray(f.readText()).let { a -> (0 until a.length()).map { a.getString(it) } }
        } catch (_: Exception) { emptyList() }
    }

    // False when the name is empty or already there.
    fun add(c: Context, name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || all.any { it.equals(n, ignoreCase = true) }) return false
        save(c, custom + n)
        return true
    }

    fun remove(c: Context, name: String) { if (name in custom) save(c, custom - name) }

    private fun save(c: Context, list: List<String>) {
        custom = list
        try { file(c).writeText(JSONArray(list).toString()) } catch (_: Exception) {}
    }
}
