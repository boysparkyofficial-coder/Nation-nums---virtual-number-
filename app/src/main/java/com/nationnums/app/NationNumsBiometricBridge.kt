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
            if (!isTrustedOrigin()) {
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

    private fun isTrustedOrigin(): Boolean {
        val current = runCatching { webView.url?.let(android.net.Uri::parse) }.getOrNull()
        return current?.scheme == "https" &&
            current.host.equals("nation-nums.base44.app", ignoreCase = true)
    }

    @JavascriptInterface
    fun isBiometricAvailable(): Boolean {
        return isTrustedOrigin() &&
            androidx.biometric.BiometricManager.from(activity).canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    // Backward-compatible alias for the first native implementation.
    @JavascriptInterface
    fun isAvailable(): Boolean = isBiometricAvailable()

    /** Sync the website's authenticated biometric preference to the native foreground lock. */
    @JavascriptInterface
    fun setBiometricLockEnabled(enabled: Boolean) {
        activity.runOnUiThread {
            if (isTrustedOrigin()) {
                AppLockStore(activity).setBiometricEnabled(enabled)
            }
        }
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
