package com.koolicream.zelu

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
            Text("Zelu is locked", color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText("Use your phone's PIN, pattern or fingerprint.", 13)
            Spacer(Modifier.height(22.dp))
            Cta("Unlock", onUnlock)
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
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(600); shown = true }
                    val a by animateFloatAsState(if (shown) 1f else 0f, tween(1000), label = "sms")
                    Box(Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - 0.55f * a }.clip(RoundedCornerShape(16.dp))
                        .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Column {
                            Text("HDFCBK", color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Rs.450 spent on card 9065 at SWIGGY on 16-10-26", color = ZColor.Ink, fontSize = 13.sp)
                        }
                    }
                    Text("↓", color = ZColor.Accent, fontSize = 24.sp, modifier = Modifier.graphicsLayer { alpha = a })
                    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationY = (1f - a) * 30f }.clip(RoundedCornerShape(16.dp))
                        .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        MerchantTile("Swiggy", "Food", 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Swiggy", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            MutedText("16 Oct · Food", 11)
                        }
                        Text("−₹450", color = ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Your bank texts already know where your money went.", color = ZColor.Ink,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Cta("Get started", onNext)
                }
                Stage.PRIVACY -> {
                    Text("Nothing leaves your phone.", color = ZColor.Ink, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    PromiseCard("Zelu reads", true, listOf("Bank and card alerts", "UPI payment messages", "Bill and EMI reminders"))
                    PromiseCard("Zelu skips", false, listOf("OTPs and passwords", "Personal chats", "Messages from people"))
                    Text("No account. No login. No upload.", color = ZColor.Muted, fontSize = 14.sp)
                    Cta("Continue", onNext)
                }
                Stage.PERM -> {
                    Text("Two quick permissions", color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                        Text("Why Zelu asks for SMS", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Your bank already texts you every payment. Zelu reads those texts to find the amount, who was paid and which account, so you never type anything.", color = ZColor.InkSoft, fontSize = 13.sp)
                        Text("It skips OTPs and personal chats. Your messages are never sent anywhere: Zelu has no ads and no internet access.", color = ZColor.InkSoft, fontSize = 13.sp)
                    }
                    Cta(if (smsGiven) "Allowed" else "Allow both", onAllow)
                    if (denied) Text("SMS is needed to read your bank alerts. Allow it in Settings › Apps › Zelu › Permissions, then reopen Zelu.",
                        color = ZColor.Warn, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Stage.SCAN -> {
                    // "Reading your messages" worried people. Say what is looked for, what is skipped, and where it stays,
                    // one line at a time while the alerts are read.
                    var lines by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) { repeat(3) { delay(500); lines++ } }
                    LogoMark(66f)
                    Text("Finding your payments", color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    MutedText("Usually a few seconds.", 14)
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
                    Text("All set.", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%,d".format(p), color = ZColor.Accent, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                        Text(if (stats.first == 1) "payment found" else "payments found", color = ZColor.Muted, fontSize = 15.sp)
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(b, if (b == 1) "bank account" else "bank accounts", Modifier.weight(1f))
                        StatTile(c, if (c == 1) "card" else "cards", Modifier.weight(1f))
                    }
                    if (accountsNext) MutedText("Next, tick which of these are yours.", 14)
                    Cta(if (accountsNext) "Check my accounts" else "Open Zelu", onNext)
                    Text("Read on this phone. Nothing was uploaded.", color = ZColor.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
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
