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
                    Spacer(Modifier.height(6.dp))
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
