package com.koolicream.zelu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable internal fun AcctLogo(key: String, size: Int = 38) {
    val logo = bankLogoRes(key)
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.29f).dp)).background(Color.White), contentAlignment = Alignment.Center) {
        if (logo != null) androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo),
            contentDescription = null, modifier = Modifier.size((size * 0.68f).dp))
        else BankMark(key)
        NetBadge(key, (size * 0.42f).dp, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable internal fun SheetButton(text: String, main: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(if (main) ZColor.Accent else ZColor.Card).clickable { onClick() }.padding(vertical = 13.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (main) ZColor.AccentInk else ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable internal fun Cta(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(26.dp)).background(ZColor.Accent)
            .clickable { onClick() }.padding(horizontal = 30.dp, vertical = 13.dp),
    ) { Text(text, color = ZColor.AccentInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable internal fun OnbPoint(title: String, sub: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(ZColor.Accent))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = ZColor.Muted, fontSize = 12.sp)
        }
    }
}

@Composable internal fun BrandBar() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoMark(30f)
        Spacer(Modifier.width(9.dp))
        Row {
            Text("Ze", color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp)
            Text("lu", color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        }
    }
}

// Wraps an account or card tile. "Mark as closed" sits behind a small ⋯ in the corner instead of a line of text under
// every tile. A closed one is frozen: a cool blue wash, and its ❄ tag reopens it.
@Composable internal fun ClosableFrame(
    closed: Boolean, radius: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, onToggle: () -> Unit, content: @Composable () -> Unit,
) {
    val ice = Color(0xFF6FA8D6)
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        content()
        if (closed) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(radius)).background(ice.copy(alpha = 0.34f)))
            Text("❄ Closed · Reopen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(8.dp)).background(ice)
                    .clickable { onToggle() }.padding(horizontal = 8.dp, vertical = 3.dp))
        } else Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)).clickable { menu = true }
                .buttonLabel("More"), contentAlignment = Alignment.Center) {
                Text("⋯", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
            }
            androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                androidx.compose.material3.DropdownMenuItem(text = { Text("Mark as closed") }, onClick = { menu = false; onToggle() })
            }
        }
    }
}

// A small ⓘ: tap it to read what a number means. Explanations live here instead of under every card.
@Composable internal fun InfoDot(text: String, title: String = "") {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.size(28.dp).clip(CircleShape).clickable { open = true }.buttonLabel("What this means"), contentAlignment = Alignment.Center) {
        Text("ⓘ", color = ZColor.Muted, fontSize = 15.sp, modifier = Unspoken)
    }
    if (open) androidx.compose.ui.window.Dialog(onDismissRequest = { open = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card).padding(20.dp)) {
            if (title.isNotEmpty()) { Text(title, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)) }
            Text(text, color = ZColor.InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            Text("OK", color = ZColor.Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(8.dp)).clickable { open = false }.padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

// A hint shown once, the first time, with "Got it"; never again after that.
@Composable internal fun OnceHint(key: String, text: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val sp = remember { ctx.getSharedPreferences("zelu_hints", android.content.Context.MODE_PRIVATE) }
    var seen by remember { mutableStateOf(sp.getBoolean(key, false)) }
    if (seen) return
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.OrangeSoft).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = ZColor.InkSoft, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("Got it", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { sp.edit().putBoolean(key, true).apply(); seen = true }
                .padding(horizontal = 6.dp, vertical = 4.dp))
    }
}

// For TalkBack: a tap target that shows only a symbol or an icon is read by this label, as a button.
internal fun Modifier.buttonLabel(label: String): Modifier =
    semantics { contentDescription = label; role = androidx.compose.ui.semantics.Role.Button }

// What TalkBack says for something that shows its meaning only by a symbol or a colour.
internal fun Modifier.spoken(label: String): Modifier = semantics { contentDescription = label }

// A symbol drawn for the eye ("‹", "✓") that TalkBack would otherwise read out.
internal val Unspoken: Modifier = Modifier.clearAndSetSemantics {}

// A row that ticks on and off, read by TalkBack as a checkbox with its state.
internal fun Modifier.checkRow(on: Boolean, flip: () -> Unit): Modifier =
    toggleable(value = on, role = androidx.compose.ui.semantics.Role.Checkbox) { flip() }

// One option in a list where one is picked, read as selected or not.
internal fun Modifier.choice(selected: Boolean, pick: () -> Unit): Modifier =
    selectable(selected = selected, role = androidx.compose.ui.semantics.Role.RadioButton) { pick() }

// label names the button for TalkBack; dotLabel says what the dot means when it shows.
@Composable internal fun TopIcon(label: String, onClick: () -> Unit, dot: Boolean = false, dotLabel: String = "", content: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).clickable { onClick() }
            .buttonLabel(if (dot && dotLabel.isNotEmpty()) "$label, $dotLabel" else label),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
    }
}

@Composable internal fun FunnelIcon() {
    val c = ZColor.Ink
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.1f); lineTo(w * 0.95f, h * 0.1f); lineTo(w * 0.6f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.9f); lineTo(w * 0.4f, h * 0.78f); lineTo(w * 0.4f, h * 0.5f); close()
        }
        drawPath(path, c, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
    }
}

@Composable internal fun MenuIcon() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { Box(Modifier.width(16.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(ZColor.Ink)) }
    }
}

@Composable internal fun MenuRow(
    title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = ZColor.Accent, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = ZColor.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
        androidx.compose.material3.Icon(Icons.Rounded.ChevronRight, null, tint = ZColor.Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable internal fun PickChip(label: String, selected: Boolean, lead: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) ZColor.Ink else ZColor.Card)
            .border(1.dp, if (selected) ZColor.Ink else ZColor.Line, RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(start = if (lead != null) 6.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lead != null) { lead(); Spacer(Modifier.width(7.dp)) }
        Text(label, color = if (selected) ZColor.Paper else ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable internal fun MenuSection(label: String) {
    Text(label.uppercase(), color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp))
}

@Composable internal fun BottomNav(view: String, modifier: Modifier, onPick: (String) -> Unit) {
    val items = listOf(
        Triple("ledger", "Home", Icons.Rounded.Home), Triple("spend", "Spend", Icons.Rounded.PieChart),
        Triple("payees", "Payees", Icons.Rounded.People), Triple("dues", "Dues", Icons.Rounded.CalendarMonth),
    )
    Row(
        modifier.fillMaxWidth().background(ZColor.Card).border(1.dp, ZColor.Line)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { (k, label, icon) ->
            val on = view == k
            val pill by animateColorAsState(if (on) ZColor.Ink else ZColor.Card, tween(200), label = "tab")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(pill)
                    .clickable { onPick(k) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val tint by animateColorAsState(if (on) ZColor.Paper else ZColor.Muted, tween(200), label = "tabTint")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(label, color = tint, fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

@Composable internal fun NavButton(symbol: String, label: String = if (symbol == "‹") "Previous" else "Next", onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).clickable { onClick() }.buttonLabel(label),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = ZColor.Ink, fontSize = 20.sp, modifier = Unspoken) }
}

@Composable internal fun DateChip(value: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(10.dp)).clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) { Text(value, color = ZColor.Ink, fontSize = 13.sp) }
}

@Composable internal fun DropdownField(
    label: String, valueLabel: String, options: List<Pair<String, String>>,
    catIcons: Boolean = false, valueKey: String = "", leadIcon: ((String) -> Pair<androidx.compose.ui.graphics.vector.ImageVector, Color>)? = null,
    onPick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZColor.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(92.dp))
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.Card)
                    .border(1.dp, ZColor.Line, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (catIcons) { CatDot(valueKey, 26.dp); Spacer(Modifier.width(10.dp)) }
                else if (leadIcon != null) { leadIcon(valueKey).let { (ic, tn) -> IconDot(ic, tn, 26.dp) }; Spacer(Modifier.width(10.dp)) }
                Text(valueLabel, color = ZColor.Ink, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("▾", color = ZColor.Muted, fontSize = 12.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.background(ZColor.Card).heightIn(max = 420.dp)) {
                options.forEach { (v, l) ->
                    DropdownMenuItem(
                        text = { Text(l, color = ZColor.Ink, fontWeight = if (v == valueKey) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (catIcons) ({ CatDot(v, 28.dp) }) else if (leadIcon != null) ({ leadIcon(v).let { (ic, tn) -> IconDot(ic, tn, 28.dp) } }) else null,
                        onClick = { onPick(v); expanded = false })
                }
            }
        }
    }
}

@Composable internal fun IconDot(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.58f))
    }
}

// Small tinted tile with the category's icon ("All" gets a generic grid icon).
@Composable internal fun CatDot(cat: String, size: androidx.compose.ui.unit.Dp) {
    val all = cat == "All" || cat.isBlank()
    val tint = if (all) ZColor.Ink else catColor(cat)
    val icon = if (all) Icons.Rounded.Category else CatIcons.forCategory(cat)
    Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center) {
        if (icon != null) androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.58f))
        else Text(cat.take(1).uppercase(), color = tint, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable internal fun ChipScroll(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable internal fun Chip(label: String, selected: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    val bg = if (selected && filled) ZColor.Accent else ZColor.Card
    val border = if (selected) (if (filled) ZColor.Accent else ZColor.Ink) else ZColor.Line
    val fg = if (selected && filled) ZColor.AccentInk else if (selected) ZColor.Ink else ZColor.Muted
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable internal fun Panel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(18.dp))
            .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp)).padding(18.dp),
        content = content,
    )
}

@Composable internal fun Eyebrow(text: String) {
    Text(text.uppercase(), color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable internal fun MutedText(text: String, size: Int = 13) {
    Text(text, color = ZColor.Muted, fontSize = size.sp)
}

@Composable internal fun Meter(fraction: Float, color: Color = ZColor.Ink) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(ZColor.Line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(color))
    }
}

@Composable internal fun Fact(k: String, v: String, color: Color = ZColor.Ink) {
    Column {
        MutedText(k, 12)
        Text(v, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable internal fun TonePill(text: String, fg: Color, bg: Color) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable internal fun Donut(segs: List<Pair<Color, Float>>, centerText: String, caption: String = "total",
                              dia: androidx.compose.ui.unit.Dp = 190.dp, ring: androidx.compose.ui.unit.Dp = 28.dp,
                              onSliceTap: (Int) -> Unit = {}) {
    Box(Modifier.size(dia), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(dia).pointerInput(segs) {
                detectTapGestures { off ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val ang = Math.toDegrees(atan2((off.y - cy).toDouble(), (off.x - cx).toDouble())).toFloat()
                    val a = (ang + 90f + 360f) % 360f
                    var acc = 0f
                    for (i in segs.indices) {
                        val sweep = segs[i].second * 360f
                        if (a >= acc && a < acc + sweep) { onSliceTap(i); break }
                        acc += sweep
                    }
                }
            }
        ) {
            val stroke = ring.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(ZColor.Paper, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            var start = -90f
            segs.forEach { (color, frac) ->
                val sweep = frac * 360f
                drawArc(color, start, sweep, false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, color = ZColor.Ink, fontSize = if (centerText.length > 8) 15.sp else 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            MutedText(caption, 11)
        }
    }
}

// ---------- Payees ----------

// Squarified treemap in a 100 x 100 box: each value gets a block whose area matches its share.
internal data class TmCell(val idx: Int, val x: Float, val y: Float, val w: Float, val h: Float)
internal fun treemap(values: List<Double>): List<TmCell> {
    val total = values.sum()
    if (total <= 0.0) return emptyList()
    val areas = values.map { (it / total * 10000.0).toFloat() }
    val out = ArrayList<TmCell>()
    var x = 0f; var y = 0f; var w = 100f; var h = 100f
    var i = 0
    fun worst(row: List<Int>, sum: Float, side: Float): Float {
        var m = 0f
        row.forEach { r -> val a = areas[r]; val q = maxOf(side * side * a / (sum * sum), sum * sum / (side * side * a)); if (q > m) m = q }
        return m
    }
    while (i < areas.size) {
        val side = minOf(w, h)
        val row = ArrayList<Int>(); var sum = 0f; var best = Float.MAX_VALUE
        var k = i
        while (k < areas.size) {
            val cs = sum + areas[k]
            val wr = worst(row + k, cs, side)
            if (row.isNotEmpty() && wr > best) break
            row.add(k); sum = cs; best = wr; k++
        }
        i += row.size
        val thick = sum / side; var off = 0f
        if (w >= h) { row.forEach { r -> val hh = areas[r] / thick; out.add(TmCell(r, x, y + off, thick, hh)); off += hh }; x += thick; w -= thick }
        else { row.forEach { r -> val ww = areas[r] / thick; out.add(TmCell(r, x + off, y, ww, thick)); off += ww }; y += thick; h -= thick }
    }
    return out
}

@Composable internal fun Pill(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(ZColor.Paper)
            .border(1.dp, ZColor.Line, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
    ) { Text(text, color = ZColor.InkSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

// ---------- Pace ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun AccountChips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { a ->
            val sel = a == selected
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) ZColor.Ink else ZColor.Card)
                    .border(1.dp, if (sel) ZColor.Ink else ZColor.Line, RoundedCornerShape(20.dp))
                    .clickable { onPick(a) }.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    val logo = if (a == "All") null else bankLogoRes(a)
                    when {
                        a == "All" -> androidx.compose.material3.Icon(Icons.Rounded.AccountBalance, null, tint = ZColor.Ink, modifier = Modifier.size(14.dp))
                        logo != null -> androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(logo), null, modifier = Modifier.size(16.dp))
                        else -> Text(a.take(1).uppercase(), color = bankColor(a), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(7.dp))
                Text(accountLabel(a), color = if (sel) ZColor.Paper else ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---------- Trends ----------
