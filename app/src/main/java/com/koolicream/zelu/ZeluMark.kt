package com.koolicream.zelu

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// The Zelu mark ("Ledger Z"), as on the launcher icon (res/drawable/ic_launcher_fg.xml, colours in
// ic_launcher_colors.xml): a cream Z whose slanted line runs on as two golden lines, on espresso. Drawn here for the
// app's own screens and the home-screen widget. Coordinates are in a 72-unit square, the part of the launcher grid
// a phone shows.
object ZeluMark {
    const val TOP = 0xFF3B2F25
    const val BOTTOM = 0xFF211A14
    const val STROKE = 7f

    // Each line: colour, then the points it runs through (x, y pairs).
    val LINES = listOf(
        0xFFF4EADC to floatArrayOf(15f, 16f, 57f, 16f, 17f, 56f),
        0xFFF2C27A to floatArrayOf(57f, 31f, 32f, 56f),
        0xFFDDA35A to floatArrayOf(57f, 46f, 47f, 56f),
    )

    /** The mark on its rounded tile, for the widget (a widget can't draw Compose). */
    fun bitmap(px: Int): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(b)
        val s = px.toFloat()
        val k = s / 72f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, s, s, TOP.toInt(), BOTTOM.toInt(), Shader.TileMode.CLAMP) }
        c.drawRoundRect(RectF(0f, 0f, s, s), s * 0.24f, s * 0.24f, bg)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; strokeWidth = STROKE * k
        }
        for ((col, p) in LINES) {
            ink.color = col.toInt()
            c.drawPath(android.graphics.Path().apply { moveTo(p[0] * k, p[1] * k); for (i in 2 until p.size step 2) lineTo(p[i] * k, p[i + 1] * k) }, ink)
        }
        return b
    }
}

@Composable internal fun LogoMark(sizeDp: Float) {
    Canvas(Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.24f).dp))) {
        val k = size.width / 72f
        drawRect(Brush.linearGradient(listOf(Color(ZeluMark.TOP), Color(ZeluMark.BOTTOM)), Offset.Zero, Offset(size.width, size.height)))
        for ((col, p) in ZeluMark.LINES) {
            drawPath(Path().apply { moveTo(p[0] * k, p[1] * k); for (i in 2 until p.size step 2) lineTo(p[i] * k, p[i + 1] * k) }, Color(col),
                style = Stroke(width = ZeluMark.STROKE * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
