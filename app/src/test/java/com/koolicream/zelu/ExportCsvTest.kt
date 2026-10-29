package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportCsvTest {
    @Test fun plainTextIsQuoted() {
        assertEquals("\"SWIGGY\"", Exporter.esc("SWIGGY"))
        assertEquals("\"\"", Exporter.esc(""))
        assertEquals("\"Say \"\"hi\"\"\"", Exporter.esc("Say \"hi\""))
    }

    // A payee read from a spoofed SMS must not run as a formula when the file is opened in a spreadsheet.
    @Test fun formulaStartsAreMadeText() {
        assertEquals("\"'=HYPERLINK(\"\"http://x\"\")\"", Exporter.esc("=HYPERLINK(\"http://x\")"))
        assertEquals("\"'+91 98\"", Exporter.esc("+91 98"))
        assertEquals("\"'-5\"", Exporter.esc("-5"))
        assertEquals("\"'@SUM(A1)\"", Exporter.esc("@SUM(A1)"))
        assertEquals("\"'\tx\"", Exporter.esc("\tx"))
        // Only the first character matters.
        assertEquals("\"A=B\"", Exporter.esc("A=B"))
    }
}
