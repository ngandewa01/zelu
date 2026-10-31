package com.koolicream.zelu

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.MoveToInbox
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.automirrored.rounded.Label
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
import androidx.compose.foundation.verticalScroll
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
            Text(stringResource(R.string.ze), color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp)
            Text(stringResource(R.string.lu), color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = 23.sp)
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
            Text(stringResource(R.string.closed_reopen), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(8.dp)).background(ice)
                    .clickable { onToggle() }.padding(horizontal = 8.dp, vertical = 3.dp))
        } else Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)).clickable { menu = true }
                .buttonLabel("More"), contentAlignment = Alignment.Center) {
                Text("⋯", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
            }
            androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.mark_as_closed)) }, onClick = { menu = false; onToggle() })
            }
        }
    }
}

// A small ⓘ: tap it to read what a number means. Explanations live here instead of under every card.
@Composable internal fun InfoDot(text: String, title: String = "", tint: Color = ZColor.Muted) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.size(28.dp).clip(CircleShape).clickable { open = true }.buttonLabel("What this means"), contentAlignment = Alignment.Center) {
        Text("ⓘ", color = tint, fontSize = 15.sp, modifier = Unspoken)
    }
    if (open) androidx.compose.ui.window.Dialog(onDismissRequest = { open = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card).padding(20.dp)) {
            if (title.isNotEmpty()) { Text(title, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)) }
            Text(text, color = ZColor.InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.ok), color = ZColor.Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold,
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
        Text(stringResource(R.string.got_it), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
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
        Modifier.size(40.dp).clip(CircleShape).background(ZColor.Tile)
            .border(1.dp, ZColor.Line, CircleShape).clickable { onClick() }
            .buttonLabel(if (dot && dotLabel.isNotEmpty()) "$label, $dotLabel" else label),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
    }
}

@Composable internal fun FunnelIcon() {
    val c = Color(if (ZTheme.dark) 0xFFA78BFA else 0xFF8B5CF6)
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

@Composable internal fun BottomNav(view: Tab, modifier: Modifier, onPick: (Tab) -> Unit) {
    // Each tab has its own colour on a round tile, like every other icon in the app; the chosen one gets a wash
    // of its colour and a bold name.
    data class NavItem(val key: Tab, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val light: Long, val dark: Long)
    val items = listOf(
        NavItem(Tab.LEDGER, "Home", Icons.Rounded.Home, 0xFFFF6B1A, 0xFFFF8A3D), NavItem(Tab.SPEND, "Spend", Icons.Rounded.PieChart, 0xFF8B5CF6, 0xFFA78BFA),
        NavItem(Tab.PAYEES, "Payees", Icons.Rounded.People, 0xFF22B455, 0xFF3DD171), NavItem(Tab.DUES, "Dues", Icons.AutoMirrored.Rounded.ReceiptLong, 0xFFEAB308, 0xFFFACC15),
    )
    Row(
        modifier.fillMaxWidth().background(ZColor.Card).border(1.dp, ZColor.Line)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { t ->
            val on = view == t.key
            val tint = Color(if (ZTheme.dark) t.dark else t.light)
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onPick(t.key) }.testTag("nav-" + t.key.key).padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape)
                    .background(if (on) tint.copy(alpha = if (ZTheme.dark) 0.22f else 0.16f) else ZColor.Tile)
                    .border(if (on) 1.5.dp else 1.dp, if (on) tint else ZColor.Line, CircleShape),
                    contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(t.icon, null, tint = tint, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(t.label, color = if (on) ZColor.Ink else ZColor.Muted, fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable internal fun NavButton(symbol: String, label: String = if (symbol == "‹") "Previous" else "Next", onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(ZColor.Tile)
            .border(1.dp, ZColor.Line, CircleShape).clickable { onClick() }.buttonLabel(label),
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

// Small round tile with the category's icon in its colour ("All" gets a generic grid icon).
@Composable internal fun CatDot(cat: String, size: androidx.compose.ui.unit.Dp) {
    val all = cat == "All" || cat.isBlank()
    val tint = if (all) ZColor.Ink else catColor(cat)
    val icon = if (all) Icons.Rounded.Category else CatIcons.forCategory(cat)
    Box(Modifier.size(size).clip(CircleShape).background(ZColor.Tile),
        contentAlignment = Alignment.Center) {
        if (icon != null) androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.6f))
        else Text(cat.take(1).uppercase(), color = tint, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

// Chips wrap onto as many lines as they need; a sideways scroll hides most of a long list (categories).
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun ChipScroll(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

// A Panel drawn in slices for a long list inside a LazyColumn: its rounded top and bottom ends (with the same 14dp gap
// above and 18dp padding inside), and the rows between them on the same card colour and outline.
@Composable internal fun PanelEnd(top: Boolean) {
    if (top) Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(18.dp).clipToBounds().drawBehind {
            val r = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
            val stroke = 1.dp.toPx()
            val y = if (top) 0f else -size.height
            drawRoundRect(ZColor.Card, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, size.height * 2), r)
            drawRoundRect(ZColor.Line, androidx.compose.ui.geometry.Offset(stroke / 2, y + stroke / 2),
                androidx.compose.ui.geometry.Size(size.width - stroke, size.height * 2 - stroke), r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        })
}

@Composable internal fun PanelSlice(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().drawBehind {
        drawRect(ZColor.Card)
        val s = 1.dp.toPx()
        drawLine(ZColor.Line, androidx.compose.ui.geometry.Offset(s / 2, 0f), androidx.compose.ui.geometry.Offset(s / 2, size.height), s)
        drawLine(ZColor.Line, androidx.compose.ui.geometry.Offset(size.width - s / 2, 0f), androidx.compose.ui.geometry.Offset(size.width - s / 2, size.height), s)
    }.padding(horizontal = 18.dp)) { content() }
}

@Composable internal fun Eyebrow(text: String, color: Color = ZColor.Muted) {
    Text(text.uppercase(), color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

// A screen tab: a bright icon on a round tile (same look as the category icons), then the name and an optional
// count (not shown: tabs carry only an icon and a name). The chosen tab takes a soft wash of its own icon colour with a matching outline.
@Composable internal fun IconTab(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, light: Long, dark: Long,
                                 selected: Boolean, count: Int = 0, compact: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) =
    IconTab(icon, label, Color(if (ZTheme.dark) dark else light), selected, count, compact, modifier, onClick)

@Composable internal fun IconTab(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color,
                                 selected: Boolean, count: Int = 0, compact: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) tint.copy(alpha = if (ZTheme.dark) 0.22f else 0.14f) else ZColor.Card
    val border = if (selected) tint else ZColor.Line
    val tileSize = if (compact) 24.dp else 28.dp
    val gap = if (compact) 6.dp else 7.dp
    val endPad = if (compact) 10.dp else 12.dp
    val fit = LocalTabFit.current
    // Outside a tab row (a scrolling list of choices) there is room for the whole name.
    if (fit == null) {
        Row(
            modifier.clip(RoundedCornerShape(22.dp)).background(bg)
                .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(22.dp))
                .clickable { onClick() }.padding(start = 4.dp, end = endPad, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(tileSize).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(if (compact) 16.dp else 18.dp))
            }
            Spacer(Modifier.width(gap))
            Text(label, color = if (selected) ZColor.Ink else ZColor.Muted, fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false)
        }
        return
    }
    // In a tab row the name is measured before it is drawn (in bold, so choosing a tab never makes it longer), at
    // the tab's real width: the row then picks one text size, and drops the round icons if a name can't otherwise fit.
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.clip(RoundedCornerShape(22.dp)).background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .clickable { onClick() },
    ) {
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val tabWidth = constraints.maxWidth
        val sizes = remember(label, tabWidth, density) {
            fun widthAt(sp: Float) = measurer.measure(androidx.compose.ui.text.AnnotatedString(label),
                style = androidx.compose.ui.text.TextStyle(fontSize = sp.sp, fontWeight = FontWeight.Bold),
                softWrap = false, maxLines = 1).size.width
            with(density) {
                val withIcon = tabWidth - (4.dp + tileSize + gap + endPad).roundToPx()
                val plain = tabWidth - (endPad * 2).roundToPx()
                TabFit.largestFitting(TabFit.WITH_ICON, withIcon, ::widthAt) to TabFit.largestFitting(TabFit.PLAIN, plain, ::widthAt)
            }
        }
        androidx.compose.runtime.SideEffect { fit.report(label, sizes.first, sizes.second) }
        val showIcon = fit.iconsOn
        Row(
            Modifier.fillMaxWidth().padding(start = if (showIcon) 4.dp else endPad, end = endPad, top = 4.dp, bottom = 4.dp)
                .heightIn(min = tileSize),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIcon) {
                Box(Modifier.size(tileSize).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(if (compact) 16.dp else 18.dp))
                }
                Spacer(Modifier.width(gap))
            }
            Text(label, color = if (selected) ZColor.Ink else ZColor.Muted, fontSize = fit.size.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}


// A category as an icon pill: its own icon and colour, the same as everywhere else in the app.
@Composable internal fun CatTab(cat: String, selected: Boolean, onClick: () -> Unit) {
    val icon = CatIcons.forCategory(cat)
    IconTab(icon ?: Icons.AutoMirrored.Rounded.Label, if (icon == null) customLabel(cat) else cat, catColor(cat), selected, onClick = onClick)
}

// What a payment is, other than a spending category: the same icons and colours as the Needs-you answers.
internal fun kindStyle(kind: String): Triple<androidx.compose.ui.graphics.vector.ImageVector, Long, Long> = when (kind) {
        "spending" -> Triple(Icons.Rounded.NorthEast, 0xFFFF6B1A, 0xFFFF8A3D)
        "income" -> Triple(Icons.Rounded.SouthWest, 0xFF22B455, 0xFF3DD171)
        "transfer" -> Triple(Icons.Rounded.SwapHoriz, 0xFF0D9488, 0xFF2DD4BF)
        "lent" -> Triple(Icons.AutoMirrored.Rounded.Send, 0xFF3B82F6, 0xFF60A5FA)
        "borrowed" -> Triple(Icons.Rounded.MoveToInbox, 0xFF8B5CF6, 0xFFA78BFA)
        "split" -> Triple(Icons.AutoMirrored.Rounded.CallSplit, 0xFFC026D3, 0xFFE879F9)
        "refund" -> Triple(Icons.AutoMirrored.Rounded.Undo, 0xFF16A34A, 0xFF4ADE80)
        "new" -> Triple(Icons.Rounded.Add, 0xFFA8845A, 0xFFC9B59C)
        else -> Triple(Icons.Rounded.Block, 0xFF64748B, 0xFF94A3B8)
}

@Composable internal fun KindTab(kind: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val (icon, light, dark) = kindStyle(kind)
    IconTab(icon, label, light, dark, selected, onClick = onClick)
}

// A choice in a grid: round icon over a short name, three to a row, all the same size.
internal class Pick(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val tint: Color, val selected: Boolean, val onClick: () -> Unit)

@Composable internal fun kindPick(kind: String, label: String, selected: Boolean, onClick: () -> Unit): Pick {
    val (icon, light, dark) = kindStyle(kind)
    return Pick(icon, label, Color(if (ZTheme.dark) dark else light), selected, onClick)
}

@Composable internal fun catPick(cat: String, selected: Boolean, onClick: () -> Unit): Pick {
    val icon = CatIcons.forCategory(cat)
    return Pick(icon ?: Icons.AutoMirrored.Rounded.Label, if (icon == null) customLabel(cat) else cat, catColor(cat), selected, onClick)
}

// One short list instead of many tabs: a search box, then rows of icon and name under small headings. It shows about five
// rows and scrolls; the chosen one carries a tick.
@Composable internal fun PickList(sections: List<Pair<String, List<Pick>>>) {
    var q by remember { mutableStateOf("") }
    val f = q.trim().lowercase()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Tile).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp))) {
        androidx.compose.foundation.text.BasicTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = ZColor.Ink, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(ZColor.Accent),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (q.isEmpty()) Text(stringResource(R.string.pick_search), color = ZColor.Muted, fontSize = 15.sp)
                inner()
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(ZColor.Line))
        Column(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
            var any = false
            sections.forEach { (title, items) ->
                val shown = items.filter { f.isEmpty() || it.label.lowercase().contains(f) }
                if (shown.isNotEmpty()) {
                    any = true
                    Text(title.uppercase(), color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp))
                    shown.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().background(if (p.selected) p.tint.copy(alpha = if (ZTheme.dark) 0.22f else 0.13f) else Color.Transparent)
                                .clickable { p.onClick() }.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(ZColor.Card), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(p.icon, null, tint = p.tint, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(p.label, color = ZColor.Ink, fontSize = 15.sp, fontWeight = if (p.selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (p.selected) Text("✓", color = p.tint, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            if (!any) Text(stringResource(R.string.pick_nothing), color = ZColor.Muted, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
        }
    }
}

// A row of tabs that always stays one line: the tabs share the width equally (each passes Modifier.weight(1f)).
@Composable internal fun TabRowWrap(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    // All tabs in the row use one text size: if any name needs to shrink to fit, they all shrink together.
    val fit = remember { TabFit() }
    androidx.compose.runtime.CompositionLocalProvider(LocalTabFit provides fit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

// What every tab in one row can fit: the largest text size with its round icon (null when even the smallest is too
// big) and without it. The row keeps the icons only if every name fits with them, and uses the smallest size of the
// chosen layout for all its tabs, so the row stays even.
internal class TabFit {
    private val fits = androidx.compose.runtime.mutableStateMapOf<String, Pair<Float?, Float?>>()
    fun report(label: String, withIcon: Float?, plain: Float?) { if (fits[label] != (withIcon to plain)) fits[label] = withIcon to plain }
    val iconsOn: Boolean get() = fits.values.all { it.first != null }
    val size: Float get() = (if (iconsOn) fits.values.mapNotNull { it.first } else fits.values.map { it.second ?: MIN }).minOrNull() ?: MAX

    companion object {
        const val MAX = 13f
        const val MIN = 9f
        val WITH_ICON = listOf(13f, 12.5f, 12f, 11.5f, 11f)
        val PLAIN = listOf(13f, 12.5f, 12f, 11.5f, 11f, 10.5f, 10f, 9.5f, 9f)
        /** The largest of [sizes] whose text width ([widthAt], in pixels) fits in [room] pixels, or null. */
        fun largestFitting(sizes: List<Float>, room: Int, widthAt: (Float) -> Int): Float? = sizes.firstOrNull { widthAt(it) <= room }
    }
}
internal val LocalTabFit = androidx.compose.runtime.compositionLocalOf<TabFit?> { null }

// A round answer button: a bright icon in a circle with its word underneath (used for the Needs-you answers,
// so all five fit in one row).
@Composable internal fun ActionCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, light: Long, dark: Long,
                                      selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = Color(if (ZTheme.dark) dark else light)
    Column(modifier.clip(RoundedCornerShape(14.dp)).clickable { onClick() }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(if (selected) tint.copy(alpha = if (ZTheme.dark) 0.22f else 0.14f) else ZColor.Tile)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) tint else ZColor.Line, CircleShape), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        FitText(label, if (selected) ZColor.Ink else ZColor.InkSoft, 12f, if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

// One line of text that shrinks a little until it fits, so a tab or button name is never cut off ("Acc…").
@Composable internal fun FitText(text: String, color: Color, size: Float, weight: FontWeight, modifier: Modifier = Modifier, minSize: Float = 9f) {
    var fs by remember(text, size) { mutableStateOf(size) }
    var ready by remember(text, size) { mutableStateOf(false) }
    Text(text, color = color, fontSize = fs.sp, fontWeight = weight, maxLines = 1, softWrap = false,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { r -> if (r.hasVisualOverflow && fs > minSize) fs -= 0.5f else ready = true },
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
}

// The phone's own date picker, opened on [current] (YYYY-MM-DD).
internal fun pickDate(context: android.content.Context, current: String, onPick: (String) -> Unit) {
    val p = current.split("-")
    val c = java.util.Calendar.getInstance()
    try { c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) } catch (_: Exception) {}
    android.app.DatePickerDialog(context, { _, y, m, d -> onPick("%04d-%02d-%02d".format(y, m + 1, d)) },
        c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
}

// Opens another app's screen (email, share, settings) from any context. The AppState's context is the application,
// not a screen, and Android starts a screen from there only as a new task.
internal fun android.content.Context.startScreen(intent: android.content.Intent) =
    startActivity(if (this is android.app.Activity) intent else intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
