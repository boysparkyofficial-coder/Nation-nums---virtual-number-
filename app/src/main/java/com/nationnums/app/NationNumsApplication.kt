package com.nationnums.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.adpreloading.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.adpreloading.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.adformats.appopen.AppOpenAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AdMob App Open integration. Ads are preloaded in the background and are only
 * displayed when the authenticated app UI explicitly reports that it is safe.
 */
class NationNumsApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val launches = prefs.getInt(KEY_LAUNCHES, 0) + 1
        prefs.edit().putInt(KEY_LAUNCHES, launches).apply()

        appScope.launch {
            runCatching {
                MobileAds.initialize(
                    applicationContext,
                    InitializationConfig.Builder(ADMOB_APP_ID).build()
                ) {
                    // SDK and adapter initialization completed.
                }
                val request = AdRequest.Builder(APP_OPEN_AD_UNIT_ID).build()
                AppOpenAdPreloader.start(
                    APP_OPEN_AD_UNIT_ID,
                    PreloadConfiguration(request, 1)
                )
            }.onFailure {
                Log.w(TAG, "AdMob initialization/preload unavailable: ${it.message}")
            }
        }
    }

    fun showAppOpenAdIfEligible(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Respect Google's guidance: do not show on the first couple of app opens.
        if (prefs.getInt(KEY_LAUNCHES, 0) < MIN_LAUNCHES_BEFORE_AD) return

        val now = SystemClock.elapsedRealtime()
        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0L)
        if (lastShown > 0L && now - lastShown < SHOW_COOLDOWN_MS) return
        if (!AppOpenAdPreloader.isAdAvailable(APP_OPEN_AD_UNIT_ID)) return

        val ad = AppOpenAdPreloader.pollAd(APP_OPEN_AD_UNIT_ID) ?: return
        prefs.edit().putLong(KEY_LAST_SHOWN, now).apply()
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open ad displayed")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open ad dismissed")
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.w(TAG, "App Open ad failed to show: ${error.message}")
            }

            override fun onAdImpression() {
                Log.d(TAG, "App Open ad impression")
            }

            override fun onAdClicked() {
                Log.d(TAG, "App Open ad clicked")
            }
        }
        runCatching { ad.show(activity) }
            .onFailure { Log.w(TAG, "Unable to show App Open ad: ${it.message}") }
    }

    companion object {
        private const val TAG = "NationNumsAdMob"
        private const val PREFS = "nation_nums_admob"
        private const val KEY_LAUNCHES = "app_launches"
        private const val KEY_LAST_SHOWN = "last_app_open_ad_elapsed"
        private const val MIN_LAUNCHES_BEFORE_AD = 3
        // Avoid showing an ad on every brief foreground/background transition.
        private const val SHOW_COOLDOWN_MS = 4 * 60 * 60 * 1000L

        // Google's official App Open test unit. Replace with the supplied production
        // unit only in the release configuration after test verification.
        private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val ADMOB_APP_ID = "ca-app-pub-5510521281550715~8212510040"
    }
}
