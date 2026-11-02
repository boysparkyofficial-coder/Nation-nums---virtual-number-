package com.nationnums.app

import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Narrow JavaScript bridge for the first-party Nation Nums website.
 * It exposes only a biometric prompt request; it never exposes session tokens,
 * account data, or arbitrary native actions to JavaScript.
 */
internal class NationNumsBiometricBridge(
    private val activity: MainActivity,
    private val webView: WebView
) {
    private val promptInFlight = AtomicBoolean(false)

    @JavascriptInterface
    fun requestBiometricUnlock() {
        activity.runOnUiThread {
            val current = runCatching { webView.url?.let(android.net.Uri::parse) }.getOrNull()
            if (current?.scheme != "https" ||
                !current.host.equals("nation-nums.base44.app", ignoreCase = true)
            ) {
                dispatchResult(false)
                return@runOnUiThread
            }
            if (!promptInFlight.compareAndSet(false, true)) return@runOnUiThread
            activity.authenticateWebsiteBiometric { success ->
                promptInFlight.set(false)
                dispatchResult(success)
            }
        }
    }

    @JavascriptInterface
    fun isAvailable(): Boolean {
        return androidx.biometric.BiometricManager.from(activity).canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun dispatchResult(success: Boolean) {
        val value = if (success) "true" else "false"
        val script = """
            (function(ok) {
              try {
                if (typeof window.biometricUnlockResult === 'function') {
                  window.biometricUnlockResult(ok);
                }
                window.dispatchEvent(new CustomEvent('biometricUnlockResult', {
                  detail: { success: ok }
                }));
                window.dispatchEvent(new CustomEvent('nationNumsBiometricResult', {
                  detail: { success: ok }
                }));
                window.postMessage({ type: 'biometricUnlockResult', success: ok }, '*');
              } catch (_) {}
            })($value);
        """.trimIndent()
        webView.post { webView.evaluateJavascript(script, null) }
    }
}
