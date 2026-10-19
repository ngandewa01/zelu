package com.nagesh.zelu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * One AdMob banner. AdMob refuses to load an ad into a view smaller than the banner, so the full 50 dp is kept while it
 * loads. If no ad can be loaded (offline, no fill) the whole block, label included, is removed so no gap is left.
 */
@Composable
fun AdBanner(unitId: String) {
    var failed by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    if (failed) return
    Column(Modifier.fillMaxWidth().alpha(if (loaded) 1f else 0f)) {
        Text("Ad", color = ZColor.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = unitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() { loaded = true }
                            override fun onAdFailedToLoad(error: LoadAdError) { failed = true }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}
