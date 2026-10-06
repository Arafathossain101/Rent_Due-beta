package com.example.rent_due.ui.components

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Google Sample Interstitial

    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showAd(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            interstitialAd = null
            // Preload next
            loadAd(activity)
            onAdDismissed?.invoke()
        } else {
            // If ad not ready, reload and continue
            loadAd(activity)
            onAdDismissed?.invoke()
        }
    }
}
