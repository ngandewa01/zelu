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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
    onBack: () -> Unit, onNext: () -> Unit, onAllow: (sms: Boolean, notif: Boolean) -> Unit,
    smsGiven: Boolean = false, notifGiven: Boolean = false,
) {
    val step = when (stage) { Stage.PRIVACY -> 0; Stage.PERM -> 1; Stage.NOTIF -> 2; else -> -1 }
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
        if (stage == Stage.SCAN) SettingUp(scanDone, scanTotal)
        // One screen glides out as the next glides in, so the first run reads as one movement.
        else AnimatedContent(
            targetState = stage,
            modifier = Modifier.align(Alignment.Center),
            // Both screens are drawn from the same middle. Without this they are lined up top-left while the box resizes,
            // which slides the icon down and to the right as one screen hands over to the next.
            contentAlignment = Alignment.Center,
            transitionSpec = {
                // The opening screen holds the icon in the middle and Welcome keeps it there, so it dissolves rather than
                // sliding away; from Welcome on, each screen glides out as the next glides in.
                // One screen leaves before the next arrives. Overlapping them put two half-faded screens on top of each
                // other, which read as a mess; this hands over cleanly.
                val handover = initialState == Stage.SPLASH || targetState == Stage.SPLASH
                val leaving = if (handover) 220 else 190
                val comesIn = if (handover) fadeIn(tween(420, delayMillis = leaving))
                    else slideInHorizontally(tween(460, delayMillis = leaving, easing = FastOutSlowInEasing)) { it / 5 } +
                        fadeIn(tween(360, delayMillis = leaving))
                val goesOut = if (handover) fadeOut(tween(leaving))
                    else slideOutHorizontally(tween(leaving, easing = FastOutSlowInEasing)) { -it / 6 } + fadeOut(tween(leaving))
                // Nothing is clipped while the box grows from one screen's size to the next.
                ContentTransform(comesIn, goesOut,
                    sizeTransform = SizeTransform(clip = false) { _, _ -> tween(460, delayMillis = leaving, easing = FastOutSlowInEasing) })
            },
            label = "onboarding",
        ) { shownStage ->
            Column(Modifier.padding(top = 40.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (shownStage) {
                Stage.PRIVACY -> {
                    // A picture, not a list: every message stays inside the phone, which is locked, and the cloud is crossed out.
                    PrivacyPicture()
                    Text(stringResource(R.string.nothing_leaves_your_phone), color = ZColor.Ink, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Cta(stringResource(R.string.continue_label), onNext)
                }
                Stage.PERM -> {
                    // The ask shows the thing it is asking to do: messages fall into the icon and come out as payments.
                    WelcomeScene()
                    Text(stringResource(R.string.welcome_headline), color = ZColor.Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Cta(stringResource(if (smsGiven) R.string.continue_label else R.string.allow_messages)) {
                        if (smsGiven) onNext() else onAllow(true, false)
                    }
                    if (denied) Text(stringResource(R.string.sms_is_needed_to_read),
                        color = ZColor.Warn, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Stage.NOTIF -> {
                    // The same again: the nudges drop in the way they really will, then you say yes or not now.
                    NudgeScene()
                    Text(stringResource(R.string.nudge_headline), color = ZColor.Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Cta(stringResource(if (notifGiven) R.string.continue_label else R.string.allow_notifications)) {
                        if (notifGiven) onNext() else onAllow(false, true)
                    }
                    Text(stringResource(R.string.not_now), color = ZColor.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onNext() }.padding(horizontal = 18.dp, vertical = 8.dp))
                }
                else -> SplashBrand()
            }
            }
        }
        if (stage == Stage.SPLASH) SplashFooter(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

// The privacy promise as a picture: the cloud is crossed out, and every message there is sits inside the phone, locked.
@Composable private fun PrivacyPicture() {
    val lines = listOf(
        Triple("HDFCBK", R.string.welcome_sms_swiggy, true),
        Triple("MESSAGE", R.string.welcome_sms_otp, false),
        Triple("MUM", R.string.welcome_sms_chat, false),
    )
    var shown by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { repeat(lines.size) { delay(420); shown++ } }
    val closed by animateFloatAsState(if (shown >= lines.size) 1f else 0f,
        tween(600, easing = FastOutSlowInEasing), label = "privacy-lock")
    Column(Modifier.fillMaxWidth().spoken(stringResource(R.string.privacy_picture_description)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        // Nothing goes up to the cloud; it fades back as the phone closes.
        Icon(Icons.Rounded.CloudOff, null, tint = ZColor.Muted,
            modifier = Modifier.size(40.dp).graphicsLayer { alpha = 1f - 0.55f * closed })
        Spacer(Modifier.height(12.dp))
        Column(Modifier.width(200.dp).clip(RoundedCornerShape(28.dp)).background(ZColor.Card)
            .border(2.dp, ZColor.Ink, RoundedCornerShape(28.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ZColor.Line))
            Spacer(Modifier.height(12.dp))
            lines.forEachIndexed { i, (sender, text, bank) ->
                val a by animateFloatAsState(if (shown > i) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "privacy-line$i")
                Column(Modifier.padding(bottom = 7.dp).graphicsLayer { alpha = a; translationY = (1f - a) * 12.dp.toPx() }
                    .fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(ZColor.Tile)
                    .border(1.dp, if (bank) ZColor.Accent else ZColor.Line, RoundedCornerShape(11.dp))
                    .padding(horizontal = 9.dp, vertical = 7.dp).then(Unspoken)) {
                    Text(sender, color = ZColor.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(stringResource(text), color = ZColor.Ink, fontSize = 11.sp, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            // The lock closes under them, on its own line, once they are all in.
            Spacer(Modifier.height(5.dp))
            Box(Modifier.graphicsLayer { alpha = closed; scaleX = 0.6f + 0.4f * closed; scaleY = 0.6f + 0.4f * closed }
                .size(40.dp).clip(CircleShape).background(ZColor.Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = ZColor.AccentInk, modifier = Modifier.size(21.dp))
            }
        }
    }
}

// The first read: an honest count with a bar at the top, and the rest of the screen says what the app does while you wait.
@Composable private fun SettingUp(done: Int, total: Int) {
    val frac by animateFloatAsState(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f, tween(300), label = "read")
    Column(Modifier.fillMaxSize().padding(top = 26.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_reading), color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.setup_count, "%,d".format(done), "%,d".format(total)), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Line)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
        }
        SetupTips(Modifier.weight(1f).padding(top = 20.dp, bottom = 10.dp))
    }
}

// Three slides that change by themselves while the messages are read.
@Composable private fun SetupTips(modifier: Modifier = Modifier) {
    val tips = listOf(
        Triple(Icons.Rounded.AccountBalanceWallet, R.string.setup_tip1_title, R.string.setup_tip1_sub),
        Triple(Icons.Rounded.Receipt, R.string.setup_tip2_title, R.string.setup_tip2_sub),
        Triple(Icons.Rounded.PieChart, R.string.setup_tip3_title, R.string.setup_tip3_sub),
    )
    var at by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2400); at = (at + 1) % tips.size } }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
        .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.weight(1f))
        // A filmstrip: one slide moves out to the left exactly as the next moves in from the right, both whole, both the
        // same height. No fading and no resizing, so nothing drifts up or down on the way.
        AnimatedContent(
            targetState = at,
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                ContentTransform(
                    slideInHorizontally(tween(560, easing = FastOutSlowInEasing)) { it },
                    slideOutHorizontally(tween(560, easing = FastOutSlowInEasing)) { -it },
                    sizeTransform = SizeTransform(clip = false) { _, _ -> androidx.compose.animation.core.snap() },
                )
            },
            label = "tip",
        ) { i ->
            val (_, title, sub) = tips[i]
            Column(Modifier.fillMaxWidth().height(248.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                TipArt(i)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(title), color = ZColor.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(sub), color = ZColor.Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.weight(1f))
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
    // The room above and below opens out around the icon, so the screen arrives as one movement.
    val open = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { open.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val p = open.value
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height((98f * p).dp).fadeEdge(top = true)) {
            msgs.forEach { m -> androidx.compose.runtime.key(m.id) { FallingMessage(m) { msgs.remove(m) } } }
        }
        LogoMark(76f)
        Spacer(Modifier.height((8f * p).dp))
        Column(Modifier.fillMaxWidth().height((150f * p).dp).fadeEdge(top = false)) {
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

// What each Setting-up slide shows: the daily number, a bill reminder, and where the money went.
@Composable private fun TipArt(which: Int) {
    Box(Modifier.fillMaxWidth().height(132.dp), contentAlignment = Alignment.Center) {
        when (which) {
            0 -> Column(Modifier.fillMaxWidth(0.82f).clip(RoundedCornerShape(16.dp)).background(ZColor.Tile).padding(14.dp).then(Unspoken)) {
                Text(stringResource(R.string.setup_art_safe), color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(money(1240.0), color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.a_day), color = ZColor.Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Line)) {
                    Box(Modifier.weight(0.44f).fillMaxHeight().background(ZColor.Accent))
                    Box(Modifier.weight(0.16f).fillMaxHeight().background(Color(0xFFEAB308)))
                    Box(Modifier.weight(0.40f).fillMaxHeight())
                }
            }
            1 -> Column(Modifier.fillMaxWidth(0.82f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ZColor.Tile).padding(12.dp).then(Unspoken),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(ZColor.Card), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Notifications, null, tint = ZColor.Accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.nudge_bill), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(money(649.0), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth(0.84f).height(34.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Tile.copy(alpha = 0.45f))
                    .align(Alignment.CenterHorizontally))
            }
            else -> Column(Modifier.fillMaxWidth(0.82f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf(
                    Triple(R.string.cat_food, 0xFFF97316, 0.34f), Triple(R.string.cat_shopping, 0xFFF59E0B, 0.22f),
                    Triple(R.string.cat_bills, 0xFF3B82F6, 0.18f), Triple(R.string.cat_travel, 0xFF8B5CF6, 0.14f),
                ).forEach { (name, colour, share) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(name), color = ZColor.InkSoft, fontSize = 12.sp, modifier = Modifier.width(66.dp))
                        Row(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Line)) {
                            // The biggest category fills the whole bar, so the rest of it is only drawn when there is some.
                            val filled = (share / 0.34f).coerceIn(0.01f, 1f)
                            Box(Modifier.weight(filled).fillMaxHeight().background(Color(colour)))
                            if (filled < 0.999f) Box(Modifier.weight(1f - filled).fillMaxHeight())
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${(share * 100).toInt()}%", color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// The nudges, dropping in the way they really will. Three, slowly: a nudge when it matters is not a stream.
private val NUDGES = listOf(
    R.string.nudge_bill to R.string.nudge_bill_sub,
    R.string.nudge_card to R.string.nudge_card_sub,
    R.string.nudge_safe to R.string.nudge_safe_sub,
)

@Composable private fun NudgeScene() {
    var shown by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { repeat(NUDGES.size) { delay(if (it == 0) 400L else 1500L); shown++ } }
    Column(Modifier.fillMaxWidth().height(236.dp).fadeEdge(top = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The newest sits on top and pushes the rest down.
        NUDGES.indices.reversed().forEach { i ->
            androidx.compose.runtime.key(i) { NudgeRow(NUDGES[i].first, NUDGES[i].second, shown > i) }
        }
    }
}

@Composable private fun NudgeRow(title: Int, sub: Int, visible: Boolean) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.expandVertically(tween(430, easing = FastOutSlowInEasing)) +
            androidx.compose.animation.slideInVertically(tween(430, easing = FastOutSlowInEasing)) { -it / 2 } + fadeIn(tween(380)),
    ) {
        Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE9E4DD))
            .padding(horizontal = 12.dp).then(Unspoken), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { LogoMark(32f) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), color = Color(0xFF1D1B18), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(stringResource(sub), color = Color(0xFF55504A), fontSize = 12.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.nudge_now), color = Color(0xFF77716A), fontSize = 11.sp)
        }
    }
}
