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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
    stage: Stage, denied: Boolean, stats: Triple<Int, Int, Int>,
    onBack: () -> Unit, onNext: () -> Unit, onAllow: () -> Unit,
    smsGiven: Boolean = false, notifGiven: Boolean = false, accountsNext: Boolean = false,
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
                    // The Zelu icon draws itself, then a short message and the button fade in. No sample message or card:
                    // the first screen says one thing.
                    val t = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(Unit) { t.animateTo(2000f, tween(2000, easing = androidx.compose.animation.core.LinearEasing)) }
                    val ms = t.value
                    fun part(start: Float, length: Float) =
                        androidx.compose.animation.core.FastOutSlowInEasing.transform(((ms - start) / length).coerceIn(0f, 1f))
                    WelcomeMark(ms)
                    Spacer(Modifier.height(10.dp))
                    val head = part(1000f, 500f)
                    Text(stringResource(R.string.welcome_headline), color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = head; translationY = (1f - head) * 16.dp.toPx() })
                    val sub = part(1300f, 500f)
                    Text(stringResource(R.string.welcome_sub), color = ZColor.Muted, fontSize = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { alpha = sub; translationY = (1f - sub) * 16.dp.toPx() })
                    Spacer(Modifier.height(10.dp))
                    val go = part(1600f, 400f)
                    Box(Modifier.graphicsLayer { alpha = go }) { Cta(stringResource(R.string.get_started), onNext) }
                }
                Stage.PRIVACY -> {
                    Text(stringResource(R.string.nothing_leaves_your_phone), color = ZColor.Ink, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    PromiseCard("Zelu reads", true, listOf("Bank and card alerts", "UPI payment messages", "Bill and EMI reminders"))
                    PromiseCard("Zelu skips", false, listOf("OTPs and passwords", "Personal chats", "Messages from people"))
                    Text(stringResource(R.string.no_account_no_login_no), color = ZColor.Muted, fontSize = 14.sp)
                    Cta(stringResource(R.string.continue_label), onNext)
                }
                Stage.PERM -> {
                    Text(stringResource(R.string.two_quick_permissions), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "SMS" to "Read your bank alerts and turn them into spends and bills.",
                        "Notifications" to "Bill due reminders and a nudge for each payment. Optional.",
                    ).forEach { (t, d) ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                            .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
                            val given = if (t == "SMS") smsGiven else notifGiven
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(t, color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (given) Box(Modifier.size(24.dp).clip(CircleShape).background(ZColor.Income).spoken("Allowed"), contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
                                }
                            }
                            MutedText(d, 13)
                        }
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.why_zelu_asks_for_sms), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.your_bank_already_texts_you), color = ZColor.InkSoft, fontSize = 13.sp)
                        Text(stringResource(R.string.it_skips_otps_and_personal), color = ZColor.InkSoft, fontSize = 13.sp)
                    }
                    Cta(if (smsGiven) "Allowed" else "Allow both", onAllow)
                    if (denied) Text(stringResource(R.string.sms_is_needed_to_read),
                        color = ZColor.Warn, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Stage.SCAN -> {
                    // "Reading your messages" worried people. Say what is looked for, what is skipped, and where it stays,
                    // one line at a time while the alerts are read.
                    var lines by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) { repeat(3) { delay(500); lines++ } }
                    LogoMark(66f)
                    Text(stringResource(R.string.finding_your_payments), color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    MutedText(stringResource(R.string.usually_a_few_seconds), 14)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Only bank, card and UPI alerts", "OTPs and personal chats skipped", "Stays on this phone").forEachIndexed { i, t ->
                            val a by animateFloatAsState(if (lines > i) 1f else 0f, tween(400), label = "line$i")
                            Box(Modifier.graphicsLayer { alpha = a }) { PromiseRow(t, true) }
                        }
                    }
                    CircularProgressIndicator(color = ZColor.Accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                }
                Stage.DONE -> {
                    var go by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { go = true }
                    val p by animateIntAsState(if (go) stats.first else 0, tween(1400), label = "p")
                    val b by animateIntAsState(if (go) stats.second else 0, tween(1400), label = "b")
                    val c by animateIntAsState(if (go) stats.third else 0, tween(1400), label = "c")
                    Text(stringResource(R.string.all_set), color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%,d".format(p), color = ZColor.Accent, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                        Text(if (stats.first == 1) "payment found" else "payments found", color = ZColor.Muted, fontSize = 15.sp)
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(b, if (b == 1) "bank account" else "bank accounts", Modifier.weight(1f))
                        StatTile(c, if (c == 1) "card" else "cards", Modifier.weight(1f))
                    }
                    if (accountsNext) MutedText(stringResource(R.string.next_tick_which_of_these), 14)
                    Cta(if (accountsNext) "Check my accounts" else "Open Zelu", onNext)
                    Text(stringResource(R.string.read_on_this_phone_nothing), color = ZColor.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                else -> SplashBrand()
            }
        }
        if (stage == Stage.SPLASH) SplashFooter(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

// One line of what Zelu does with your messages: a round tick (or cross), then the words.
@Composable private fun PromiseRow(text: String, yes: Boolean) {
    val tint = if (yes) ZColor.Income else ZColor.Warn
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(if (yes) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = tint, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = ZColor.InkSoft, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

// What Zelu reads, or skips. Both cards are full width with three lines each, so they match.
@Composable private fun PromiseCard(title: String, yes: Boolean, items: List<String>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
        .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = if (yes) ZColor.Income else ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        items.forEach { PromiseRow(it, yes) }
    }
}

// A count with its name under it, for the "All set" screen; two side by side are the same size.
@Composable private fun StatTile(n: Int, label: String, modifier: Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
        .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n.toString(), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ZColor.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// The Zelu icon on the welcome screen: it pops in and draws itself, then two soft rings spread out from it, over and over.
@Composable private fun WelcomeMark(ms: Float) {
    fun part(start: Float, length: Float, ease: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing) =
        ease.transform(((ms - start) / length).coerceIn(0f, 1f))
    val pop = part(0f, 450f, androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.25f))
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "rings")
    val spread by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spread")
    val ring = ZColor.Accent
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            if (ms > 900f) for (i in 0..1) {
                val p = (spread + i * 0.5f) % 1f
                drawCircle(ring.copy(alpha = 0.28f * (1f - p)), radius = size.minDimension * (0.29f + 0.21f * p),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            }
        }
        Box(Modifier.graphicsLayer { alpha = part(0f, 450f); scaleX = 0.6f + 0.4f * pop; scaleY = 0.6f + 0.4f * pop }) {
            LogoMark(108f, floatArrayOf(part(300f, 520f), part(700f, 280f), part(840f, 220f)))
        }
    }
}
