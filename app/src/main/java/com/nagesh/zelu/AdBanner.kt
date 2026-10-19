package com.nagesh.zelu

import androidx.compose.foundation.layout.Box
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

/** One AdMob banner. It takes no space until an ad has actually loaded, so a failed or offline load leaves no gap. */
@Composable
fun AdBanner(unitId: String) {
    var loaded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(if (loaded) 50.dp else 1.dp), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = unitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() { loaded = true }
                        override fun onAdFailedToLoad(error: LoadAdError) { loaded = false }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
