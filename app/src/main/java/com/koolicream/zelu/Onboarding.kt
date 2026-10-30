package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable internal fun LockScreen(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ZColor.Paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(72f)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.zelu_is_locked), color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText(stringResource(R.string.use_your_phone_s_pin), 13)
            Spacer(Modifier.height(22.dp))
            Cta(stringResource(R.string.unlock), onUnlock)
        }
    }
}

@Composable internal fun OnboardFlow(
    stage: Stage, denied: Boolean, scanDone: Int, scanTotal: Int,
    onBack: () -> Unit, onNext: () -> Unit, onAllow: () -> Unit,
    smsGiven: Boolean = false, notifGiven: Boolean = false,
) {
    val step = when (stage) { Stage.WELCOME -> 0; Stage.PRIVACY -> 1; Stage.PERM -> 2; else -> -1 }
    Box(Modifier.fillMaxSize().background(ZColor.Paper).padding(28.dp)) {
        if (step >= 0) Row(Modifier.align(Alignment.TopCenter).padding(top = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = step > 0) { onBack() }
                .then(if (step > 0) Modifier.buttonLabel("Back") else Unspoken), contentAlignment = Alignment.Center) {
                if (step > 0) Text("‹", color = ZColor.Ink, fontSize = 24.sp, modifier = Unspoken)
            }
            Spacer(Modifier.width(10.dp))
            (0..2).forEach { i ->
                Box(Modifier.weight(1f).padding(horizontal = 3.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= step) ZColor.Ink else ZColor.Line))
            }
            Spacer(Modifier.width(46.dp))
        }
        Column(Modifier.align(Alignment.Center).padding(top = 40.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (stage) {
                Stage.WELCOME -> {
                    // The Zelu icon in the middle. Messages come down from the top: the bank ones go into the icon, OTPs, chats
                    // and offers fade out on the way. Payment cards with their logos come out below. Then one headline and one line.
                    val t = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(Unit) { t.animateTo(2000f, tween(2000, easing = androidx.compose.animation.core.LinearEasing)) }
                    val ms = t.value
                    fun part(start: Float, length: Float) =
                        androidx.compose.animation.core.FastOutSlowInEasing.transform(((ms - start) / length).coerceIn(0f, 1f))
                    WelcomeScene()
                    val head = part(600f, 500f)
                    Text(stringResource(R.string.welcome_headline), color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = head; translationY = (1f - head) * 16.dp.toPx() })
                    val sub = part(900f, 500f)
                    Text(stringResource(R.string.welcome_sub), color = ZColor.Muted, fontSize = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { alpha = sub; translationY = (1f - sub) * 16.dp.toPx() })
                    Spacer(Modifier.height(6.dp))
                    val go = part(1300f, 400f)
                    Box(Modifier.graphicsLayer { alpha = go }) { Cta(stringResource(R.string.get_started), onNext) }
                }
                Stage.PRIVACY -> {
                    // A picture, not a list: the cloud is crossed out, the bank alert comes in, OTPs and chats stay out.
                    PrivacyPicture()
                    Text(stringResource(R.string.nothing_leaves_your_phone), color = ZColor.Ink, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Cta(stringResource(R.string.continue_label), onNext)
                }
                Stage.PERM -> {
                    // Two short rows: what, and whether it is needed. The switch shows what the phone has allowed.
                    Text(stringResource(R.string.two_quick_permissions), color = ZColor.Ink, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    PermissionPreview(smsGiven, notifGiven)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PermissionRow(Icons.Rounded.Sms, stringResource(R.string.perm_messages), stringResource(R.string.perm_needed), smsGiven)
                        PermissionRow(Icons.Rounded.Notifications, stringResource(R.string.perm_notifications), stringResource(R.string.perm_optional), notifGiven)
                    }
                    Spacer(Modifier.height(6.dp))
                    Cta(stringResource(if (smsGiven) R.string.perm_allowed else R.string.perm_allow_both), onAllow)
                    if (denied) Text(stringResource(R.string.sms_is_needed_to_read),
                        color = ZColor.Warn, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Stage.SCAN -> SettingUp(scanDone, scanTotal)
                else -> SplashBrand()
            }
        }
        if (stage == Stage.SPLASH) SplashFooter(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

// The privacy promise as a picture: no cloud, a locked phone, and three kinds of message with what happens to each.
@Composable private fun PrivacyPicture() {
    var shown by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { repeat(3) { delay(450); shown++ } }
    Column(Modifier.fillMaxWidth().spoken(stringResource(R.string.privacy_picture_description)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Rounded.CloudOff, null, tint = ZColor.Muted, modifier = Modifier.size(48.dp))
        Box(Modifier.size(width = 132.dp, height = 150.dp).clip(RoundedCornerShape(26.dp)).background(ZColor.Card)
            .border(2.dp, ZColor.Ink, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = ZColor.Accent, modifier = Modifier.size(32.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrivacyChip(Icons.Rounded.Check, R.string.privacy_bank_alert, true, shown > 0)
            PrivacyChip(Icons.Rounded.VisibilityOff, R.string.privacy_otp, false, shown > 1)
            PrivacyChip(Icons.Rounded.VisibilityOff, R.string.privacy_chat, false, shown > 2)
        }
    }
}

@Composable private fun PrivacyChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, ok: Boolean, visible: Boolean) {
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "privacy-chip")
    val tint = if (ok) ZColor.Income else ZColor.Muted
    Row(Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 12.dp.toPx() }.clip(RoundedCornerShape(20.dp))
        .background(tint.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(label), color = if (ok) ZColor.Ink else ZColor.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Unspoken)
    }
}

// One permission: a round icon, its name with "needed" or "optional" under it, and a switch that shows what is allowed.
@Composable private fun PermissionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tag: String, on: Boolean) {
    val slide by animateFloatAsState(if (on) 1f else 0f, tween(300), label = "perm-switch")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
        .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ZColor.Accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(tag.uppercase(), color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        Box(Modifier.width(48.dp).height(28.dp).clip(CircleShape).background(if (on) ZColor.Income else ZColor.Line)
            .then(if (on) Modifier.spoken(stringResource(R.string.perm_allowed)) else Unspoken)) {
            Box(Modifier.padding(3.dp).offset(x = 20.dp * slide).size(22.dp).clip(CircleShape).background(Color.White))
        }
    }
}

// The first read: an honest count with a bar, and three short things the app does while you wait.
@Composable private fun SettingUp(done: Int, total: Int) {
    val frac by animateFloatAsState(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f, tween(300), label = "read")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_reading), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.setup_count, "%,d".format(done), "%,d".format(total)), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Line)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
        }
    }
    Spacer(Modifier.height(14.dp))
    SetupTips()
}

// Three slides that change by themselves while the messages are read.
@Composable private fun SetupTips() {
    val tips = listOf(
        Triple(Icons.Rounded.AccountBalanceWallet, R.string.setup_tip1_title, R.string.setup_tip1_sub),
        Triple(Icons.Rounded.Receipt, R.string.setup_tip2_title, R.string.setup_tip2_sub),
        Triple(Icons.Rounded.PieChart, R.string.setup_tip3_title, R.string.setup_tip3_sub),
    )
    var at by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2400); at = (at + 1) % tips.size } }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
        .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.animation.Crossfade(targetState = at, animationSpec = tween(500), label = "tip") { i ->
            val (icon, title, sub) = tips[i]
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = ZColor.Accent, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(title), color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(sub), color = ZColor.Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tips.indices.forEach { i ->
                Box(Modifier.height(6.dp).width(if (i == at) 16.dp else 6.dp).clip(CircleShape).background(if (i == at) ZColor.Accent else ZColor.Line))
            }
        }
    }
}

// ---- the welcome scene ----

private class WelcomeMsg(val id: Int, val sender: String, val text: Int, val bank: Boolean)
private class WelcomeCard(val id: Int, val brand: String?, val name: Int?, val label: String, val amount: Double, val income: Boolean)
// What one sample message is, and the card it becomes (none for an OTP, a chat or an offer).
private class WelcomeSample(val sender: String, val text: Int, val card: WelcomeCard?)

private val WELCOME_SAMPLES = listOf(
    WelcomeSample("HDFCBK", R.string.welcome_sms_swiggy, WelcomeCard(0, "swiggy", null, "Swiggy", 450.0, false)),
    WelcomeSample("MESSAGE", R.string.welcome_sms_otp, null),
    WelcomeSample("SBIUPI", R.string.welcome_sms_uber, WelcomeCard(0, "uber", null, "Uber", 212.0, false)),
    WelcomeSample("MUM", R.string.welcome_sms_chat, null),
    WelcomeSample("ICICIB", R.string.welcome_sms_salary, WelcomeCard(0, null, R.string.welcome_salary, "", 52000.0, true)),
    WelcomeSample("MESSAGE", R.string.welcome_sms_offer, null),
    WelcomeSample("HDFCBK", R.string.welcome_sms_amazon, WelcomeCard(0, "amazon", null, "Amazon", 1299.0, false)),
    WelcomeSample("AXISBK", R.string.welcome_sms_netflix, WelcomeCard(0, "netflix", null, "Netflix", 649.0, false)),
)

@Composable private fun WelcomeScene() {
    val msgs = remember { androidx.compose.runtime.mutableStateListOf<WelcomeMsg>() }
    val cards = remember { androidx.compose.runtime.mutableStateListOf<WelcomeCard>() }
    LaunchedEffect(Unit) {
        var n = 0
        delay(500)
        while (true) {
            val id = n++
            val s = WELCOME_SAMPLES[id % WELCOME_SAMPLES.size]
            msgs.add(WelcomeMsg(id, s.sender, s.text, s.card != null))
            s.card?.let { c ->
                launch {
                    delay(1450)
                    cards.add(0, WelcomeCard(id, c.brand, c.name, c.label, c.amount, c.income))
                    while (cards.size > 3) cards.removeAt(cards.lastIndex)
                }
            }
            delay(780)
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(98.dp).fadeEdge(top = true)) {
            msgs.forEach { m -> androidx.compose.runtime.key(m.id) { FallingMessage(m) { msgs.remove(m) } } }
        }
        LogoMark(72f)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().height(150.dp).fadeEdge(top = false)) {
            cards.forEach { c -> androidx.compose.runtime.key(c.id) { WelcomeCardRow(c) } }
        }
    }
}

// A message falls towards the icon. A bank alert is taken in (it shrinks into the icon); the rest fade away on the way.
@Composable private fun FallingMessage(m: WelcomeMsg, onDone: () -> Unit) {
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(if (m.bank) 2000 else 1500, easing = androidx.compose.animation.core.LinearEasing))
        onDone()
    }
    fun lerp(a: Float, b: Float, x: Float) = a + (b - a) * x.coerceIn(0f, 1f)
    val t = p.value
    val y = if (m.bank) (if (t < 0.66f) lerp(-46f, 60f, t / 0.66f) else lerp(60f, 98f, (t - 0.66f) / 0.34f)) else lerp(-46f, 46f, t)
    val a = if (m.bank) (if (t < 0.14f) t / 0.14f else if (t < 0.66f) 1f else 1f - (t - 0.66f) / 0.34f)
        else (if (t < 0.18f) 0.9f * t / 0.18f else lerp(0.9f, 0f, (t - 0.18f) / 0.82f))
    val sc = if (m.bank && t > 0.66f) lerp(1f, 0.3f, (t - 0.66f) / 0.34f) else 1f
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(210.dp).graphicsLayer { translationY = y.dp.toPx(); alpha = a; scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(13.dp)).background(ZColor.Card)
            .border(1.dp, if (m.bank) ZColor.Accent else ZColor.Line, RoundedCornerShape(13.dp)).padding(horizontal = 12.dp, vertical = 7.dp)
            .then(Unspoken)) {
            Text(m.sender, color = ZColor.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text(stringResource(m.text), color = ZColor.Ink, fontSize = 12.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// A payment the icon found: its logo, who, how much. It grows into the list.
@Composable private fun WelcomeCardRow(c: WelcomeCard) {
    val shown = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    androidx.compose.animation.AnimatedVisibility(shown, enter = androidx.compose.animation.expandVertically(tween(450)) + androidx.compose.animation.fadeIn(tween(450))) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).height(44.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp).then(Unspoken), verticalAlignment = Alignment.CenterVertically) {
            if (c.brand != null) BrandTile(c.brand, 30.dp)
            else Box(Modifier.size(30.dp).clip(CircleShape).background(ZColor.Income), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Payments, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(c.name?.let { stringResource(it) } ?: c.label, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text((if (c.income) "+" else "\u2212") + money(c.amount), color = if (c.income) ZColor.Income else ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Fades one edge of what is inside to nothing, so things come and go softly.
private fun Modifier.fadeEdge(top: Boolean): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val stops = if (top) arrayOf(0f to Color.Transparent, 0.28f to Color.Black, 1f to Color.Black)
            else arrayOf(0f to Color.Black, 0.62f to Color.Black, 1f to Color.Transparent)
        drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(*stops), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
    }

// A faded Home that fills in as you allow things: Messages brings the payments and the total, Notifications a bill reminder.
@Composable private fun PermissionPreview(sms: Boolean, notif: Boolean) {
    val fill by animateFloatAsState(if (sms) 1f else 0f, tween(600), label = "preview-fill")
    val total by animateFloatAsState(if (sms) 662f else 0f, tween(900, delayMillis = 500), label = "preview-total")
    val bell by animateFloatAsState(if (notif) 1f else 0f, tween(500), label = "preview-bell")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp))
        .padding(14.dp).then(Unspoken)) {
        Eyebrow(stringResource(R.string.perm_preview_title))
        Text(if (total > 0.5f) money(total.toDouble()) else "\u20B9 \u2013\u2013", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box {
            // Before Messages: grey bars where the payments will be.
            Column(Modifier.graphicsLayer { alpha = 1f - fill }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(ZColor.Tile))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Line))
                    }
                }
            }
            Column(Modifier.graphicsLayer { alpha = fill }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewRow("swiggy", "Swiggy", "\u2212" + money(450.0), false)
                PreviewRow("uber", "Uber", "\u2212" + money(212.0), false)
                PreviewRow(null, stringResource(R.string.welcome_salary), "+" + money(52000.0), true)
            }
        }
        Row(Modifier.padding(top = 10.dp).graphicsLayer { alpha = bell }.clip(RoundedCornerShape(12.dp)).background(ZColor.Tile)
            .padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Notifications, null, tint = ZColor.Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.perm_preview_bill), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(money(649.0), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun PreviewRow(brand: String?, name: String, amount: String, income: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (brand != null) BrandTile(brand, 28.dp)
        else Box(Modifier.size(28.dp).clip(CircleShape).background(ZColor.Income), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Payments, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(amount, color = if (income) ZColor.Income else ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
