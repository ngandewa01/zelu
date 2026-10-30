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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box

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

// drawn: how much of each line is drawn (0..1), for the splash animation. In dark mode the tile is a little lighter
// with a thin edge, so it doesn't vanish into a dark background.
@Composable internal fun LogoMark(sizeDp: Float, drawn: FloatArray = FULL) {
    val dark = ZTheme.dark
    Canvas(Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.24f).dp))) {
        val k = size.width / 72f
        drawRect(Brush.linearGradient(
            if (dark) listOf(Color(0xFF4A3B2E), Color(0xFF2B2219)) else listOf(Color(ZeluMark.TOP), Color(ZeluMark.BOTTOM)),
            Offset.Zero, Offset(size.width, size.height)))
        if (dark) drawRoundRect(Color(0xFF5A4A3B), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.24f),
            style = Stroke(width = 1.dp.toPx()))
        ZeluMark.LINES.forEachIndexed { i, (col, p) ->
            val part = drawn.getOrElse(i) { 1f }
            if (part <= 0f) return@forEachIndexed
            val whole = Path().apply { moveTo(p[0] * k, p[1] * k); for (j in 2 until p.size step 2) lineTo(p[j] * k, p[j + 1] * k) }
            val path = if (part >= 1f) whole else Path().also { out ->
                val m = androidx.compose.ui.graphics.PathMeasure(); m.setPath(whole, false); m.getSegment(0f, m.length * part, out, true)
            }
            drawPath(path, Color(col), style = Stroke(width = ZeluMark.STROKE * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private val FULL = floatArrayOf(1f, 1f, 1f)

// The start screen: the three strokes of the mark fly in from outside, like payments arriving, and settle into place.
// The tile then fades in behind them and the name comes up. No line under it. Phones with animations off show the
// finished mark.
@Composable internal fun SplashBrand() {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1800f, androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing)) }
    val ms = t.value
    fun part(start: Float, length: Float, ease: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing) =
        ease.transform(((ms - start) / length).coerceIn(0f, 1f))
    val dark = ZTheme.dark
    val gold = if (dark) Color(0xFFF2C27A) else Color(0xFFC98E3F)
    // Where each stroke comes in from, as a share of the mark's own size, and when.
    val from = listOf(Offset(-1.1f, 0.55f), Offset(1.15f, -0.45f), Offset(0.95f, 1.0f))
    val arrive = listOf(part(60f, 760f), part(260f, 760f), part(460f, 760f))
    val tile = part(820f, 460f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(156.dp)) {
            val mark = size.width * 0.59f          // the mark itself, with room around it for the strokes to fly in
            val k = mark / 72f
            val ox = (size.width - mark) / 2f
            val oy = (size.height - mark) / 2f
            if (tile > 0f) {
                drawRoundRect(
                    Brush.linearGradient(
                        if (dark) listOf(Color(0xFF4A3B2E), Color(0xFF2B2219)) else listOf(Color(ZeluMark.TOP), Color(ZeluMark.BOTTOM)),
                        Offset(ox, oy), Offset(ox + mark, oy + mark)),
                    topLeft = Offset(ox, oy), size = androidx.compose.ui.geometry.Size(mark, mark),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(mark * 0.24f), alpha = tile)
                if (dark) drawRoundRect(Color(0xFF5A4A3B), topLeft = Offset(ox, oy),
                    size = androidx.compose.ui.geometry.Size(mark, mark),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(mark * 0.24f), alpha = tile,
                    style = Stroke(width = 1.dp.toPx()))
            }
            ZeluMark.LINES.forEachIndexed { i, (col, p) ->
                val a = arrive[i]
                if (a <= 0f) return@forEachIndexed
                val dx = ox + from[i].x * mark * (1f - a)
                val dy = oy + from[i].y * mark * (1f - a)
                val path = Path().apply {
                    moveTo(p[0] * k + dx, p[1] * k + dy)
                    for (j in 2 until p.size step 2) lineTo(p[j] * k + dx, p[j + 1] * k + dy)
                }
                drawPath(path, Color(col), alpha = (a * 2.2f).coerceAtMost(1f),
                    style = Stroke(width = ZeluMark.STROKE * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        val name = part(1150f, 460f)
        Row(Modifier.graphicsLayer { alpha = name; translationY = (1f - name) * 14.dp.toPx() }) {
            Text("Ze", color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            Text("lu", color = gold, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        }
    }
}

// The small line at the bottom of the start screen.
