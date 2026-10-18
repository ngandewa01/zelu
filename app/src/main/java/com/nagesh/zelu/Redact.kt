package com.nagesh.zelu

// Hides the parts of a message that identify you before it goes into a problem report.
// Amounts, dates, bank names and the layout stay, because those are what is needed to fix the reading.
object Redact {
    private val LONG_NUMBER = Regex("""\d{7,}""")
    private val VPA = Regex("""\b([A-Za-z0-9._-])[A-Za-z0-9._-]*@([A-Za-z][A-Za-z0-9.-]*)""")

    fun message(text: String): String {
        val vpaHidden = VPA.replace(text) { m -> m.groupValues[1] + "***@" + m.groupValues[2] }
        return LONG_NUMBER.replace(vpaHidden) { m -> "X".repeat(m.value.length - 3) + m.value.takeLast(3) }
    }
}
