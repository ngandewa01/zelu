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

// The Zelu mark, as on the launcher icon (res/drawable/ic_launcher_fg.xml, colours in ic_launcher_colors.xml): a bold
// Z whose bottom bar ends in a coin, dark ink on a marigold gradient. Drawn here for the app's own screens and the
// home-screen widget. Coordinates are in a 72-unit square, the part of the launcher grid a phone shows.
object ZeluMark {
    const val LIGHT = 0xFFFFC94D
    const val DEEP = 0xFFFF7A2E
    const val INK = 0xFF1E1611

    // The Z as one stroke through these points (x, y pairs), then the coin: centre x, y and radius.
    val Z = floatArrayOf(17f, 19f, 54f, 19f, 18f, 53f, 38f, 53f)
    const val Z_WIDTH = 12f
    val COIN = floatArrayOf(52.5f, 53f, 7f)

    /** The mark on its rounded tile, for the widget (a widget can't draw Compose). */
    fun bitmap(px: Int): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(b)
        val s = px.toFloat()
        val k = s / 72f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, s, s, LIGHT.toInt(), DEEP.toInt(), Shader.TileMode.CLAMP) }
        c.drawRoundRect(RectF(0f, 0f, s, s), s * 0.26f, s * 0.26f, bg)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        ink.strokeWidth = Z_WIDTH * k
        c.drawPath(android.graphics.Path().apply { moveTo(Z[0] * k, Z[1] * k); for (i in 2 until Z.size step 2) lineTo(Z[i] * k, Z[i + 1] * k) }, ink)
        ink.style = Paint.Style.FILL
        c.drawCircle(COIN[0] * k, COIN[1] * k, COIN[2] * k, ink)
        return b
    }
}

@Composable internal fun LogoMark(sizeDp: Float) {
    Canvas(Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.26f).dp))) {
        val k = size.width / 72f
        drawRect(Brush.linearGradient(listOf(Color(ZeluMark.LIGHT), Color(ZeluMark.DEEP)), Offset.Zero, Offset(size.width, size.height)))
        val ink = Color(ZeluMark.INK)
        val z = ZeluMark.Z
        drawPath(Path().apply { moveTo(z[0] * k, z[1] * k); for (i in 2 until z.size step 2) lineTo(z[i] * k, z[i + 1] * k) }, ink,
            style = Stroke(width = ZeluMark.Z_WIDTH * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val coin = ZeluMark.COIN
        drawCircle(ink, coin[2] * k, Offset(coin[0] * k, coin[1] * k))
    }
}
