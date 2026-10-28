package com.koolicream.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
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
    stage: String, denied: Boolean, stats: Triple<Int, Int, Int>,
    onBack: () -> Unit, onNext: () -> Unit, onAllow: () -> Unit,
    smsGiven: Boolean = false, notifGiven: Boolean = false,
) {
    val step = when (stage) { "welcome" -> 0; "privacy" -> 1; "perm" -> 2; else -> -1 }
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
                "welcome" -> {
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
                "privacy" -> {
                    Text("Nothing leaves your phone.", color = ZColor.Ink, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            Triple("Reads", ZColor.Income, listOf("Bank & card alerts", "UPI payment messages", "Bill due reminders")),
                            Triple("Never reads", ZColor.Warn, listOf("OTPs", "Personal chats", "Anything from contacts")),
                        ).forEach { (title, color, items) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                                .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                items.forEach { Text((if (title == "Reads") "✓ " else "✕ ") + it, color = ZColor.InkSoft, fontSize = 13.sp) }
                            }
                        }
                    }
                    Text("No account. No login. No upload.", color = ZColor.Muted, fontSize = 14.sp)
                    Cta("Continue", onNext)
                }
                "perm" -> {
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
                "scan" -> {
                    LogoMark(66f)
                    Text("Reading your messages…", color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    CircularProgressIndicator(color = ZColor.Accent)
                }
                "done" -> {
                    var go by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { go = true }
                    val p by animateIntAsState(if (go) stats.first else 0, tween(1400), label = "p")
                    val b by animateIntAsState(if (go) stats.second else 0, tween(1400), label = "b")
                    val c by animateIntAsState(if (go) stats.third else 0, tween(1400), label = "c")
                    Text("All set.", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("%,d".format(p), color = ZColor.Accent, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    Text("payments found", color = ZColor.Muted, fontSize = 15.sp)
                    Text("$b bank account${if (b == 1) "" else "s"} · $c card${if (c == 1) "" else "s"}",
                        color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Cta("Open Zelu", onNext)
                }
                else -> { // splash
                    LogoMark(86f)
                    Row {
                        Text("Ze", color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 38.sp)
                        Text("lu", color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = 38.sp)
                    }
                    Text("Every rupee, tracked on your phone.", color = ZColor.Muted,
                        fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
