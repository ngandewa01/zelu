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
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
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

// The start screen, animated (about 1.5 s): the tile pops in, the Z and its two lines draw themselves, then the name,
// the line under it and "Private · works offline". Phones with animations turned off show the finished screen.
@Composable internal fun SplashBrand() {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1500f, androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing)) }
    val ms = t.value
    fun part(start: Float, length: Float, ease: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing) =
        ease.transform(((ms - start) / length).coerceIn(0f, 1f))
    val pop = part(0f, 380f, androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.25f))
    val gold = if (ZTheme.dark) Color(0xFFF2C27A) else Color(0xFFC98E3F)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { alpha = part(0f, 380f); scaleX = 0.6f + 0.4f * pop; scaleY = 0.6f + 0.4f * pop }) {
            LogoMark(92f, floatArrayOf(part(250f, 480f), part(600f, 260f), part(720f, 200f)))
        }
        Spacer(Modifier.height(22.dp))
        val name = part(900f, 420f)
        Row(Modifier.graphicsLayer { alpha = name; translationY = (1f - name) * 14.dp.toPx() }) {
            Text("Ze", color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            Text("lu", color = gold, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        }
        Spacer(Modifier.height(8.dp))
        val tag = part(1050f, 420f)
        Text("Every rupee, tracked on your phone.", color = ZColor.Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = tag; translationY = (1f - tag) * 14.dp.toPx() })
    }
}

// The small line at the bottom of the start screen.
@Composable internal fun SplashFooter(modifier: Modifier) {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { delay(1200); t.animateTo(1f, androidx.compose.animation.core.tween(400)) }
    Text("Private · works offline", color = ZColor.Muted, fontSize = 12.sp, letterSpacing = 0.6.sp,
        modifier = modifier.graphicsLayer { alpha = t.value })
}
