package com.koolicream.zelu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The month so far as one line: what has been spent each day (solid), where the month is heading (dotted) and the budget
// (dashed), all on one scale. `spentByDay[d - 1]` is the running total at the end of day d, up to `today`.
@Composable internal fun SpendLine(
    spentByDay: List<Double>, days: Int, today: Int, expected: Double, budget: Double, color: Color, ink: Color, muted: Color, grid: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val budgetWord = stringResource(R.string.chart_budget)
    val small = TextStyle(color = muted, fontSize = 10.sp)
    val bold = TextStyle(color = ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Canvas(modifier.fillMaxWidth().height(130.dp)) {
        val left = 4.dp.toPx(); val right = 62.dp.toPx(); val top = 14.dp.toPx(); val bottom = 18.dp.toPx()
        val pw = size.width - left - right
        val ph = size.height - top - bottom
        val end = spentByDay.lastOrNull() ?: 0.0
        val yMax = maxOf(expected, budget, end, 1.0) * 1.08
        fun x(d: Int) = left + pw * (d - 1) / (days - 1).coerceAtLeast(1)
        fun y(v: Double) = top + ph * (1f - (v / yMax).toFloat())
        // Baseline and a mid line.
        listOf(0.0, yMax / 2.16, yMax / 1.08).forEach { v -> drawLine(grid, Offset(left, y(v)), Offset(left + pw, y(v)), strokeWidth = 1.dp.toPx()) }
        if (budget > 0) {
            val by = y(budget)
            drawLine(muted, Offset(left, by), Offset(left + pw + 4.dp.toPx(), by), strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)))
            drawText(measurer, budgetWord, Offset(left + pw + 8.dp.toPx(), by - 14.dp.toPx()), small)
            drawText(measurer, money(budget), Offset(left + pw + 8.dp.toPx(), by - 2.dp.toPx()), bold)
        }
        if (spentByDay.isNotEmpty()) {
            val path = Path()
            spentByDay.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(1), y(v)) else path.lineTo(x(i + 1), y(v)) }
            drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (expected > 0 && today < days) {
                drawLine(color.copy(alpha = 0.8f), Offset(x(today), y(end)), Offset(x(days), y(expected)), strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 12f)))
                drawCircle(color, 3.5.dp.toPx(), Offset(x(days), y(expected)), style = Stroke(2.dp.toPx()))
                drawText(measurer, "≈ " + money(expected), Offset(x(days) + 8.dp.toPx(), y(expected) - 7.dp.toPx()),
                    TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
            drawCircle(color, 4.dp.toPx(), Offset(x(today), y(end)))
        }
        listOf(1, 10, 20, days).distinct().forEach { d ->
            val t = measurer.measure(d.toString(), small)
            drawText(t, topLeft = Offset(x(d) - t.size.width / 2f, size.height - 14.dp.toPx()))
        }
    }
}
