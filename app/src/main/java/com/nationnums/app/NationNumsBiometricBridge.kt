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

    /**
     * Keep the website preference and native foreground lock in sync.
     * Enabling is never accepted silently: Android must verify a biometric first.
     */
    @JavascriptInterface
    fun setBiometricLockEnabled(enabled: Boolean) {
        activity.runOnUiThread {
            if (!isTrustedOrigin()) return@runOnUiThread
            val store = AppLockStore(activity)
            if (!enabled) {
                store.setBiometricEnabled(false)
                dispatchPreferenceResult(true, false)
                return@runOnUiThread
            }

            val manager = androidx.biometric.BiometricManager.from(activity)
            val availability = manager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (availability == androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                val enrollIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
                        android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                } else {
                    android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                }
                runCatching { activity.startActivity(enrollIntent) }
                dispatchPreferenceResult(false, true)
                return@runOnUiThread
            }
            if (availability != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                dispatchPreferenceResult(false, false)
                return@runOnUiThread
            }

            activity.authenticateWebsiteBiometric { success ->
                if (success && isTrustedOrigin()) store.setBiometricEnabled(true)
                dispatchPreferenceResult(success, false)
            }
        }
    }

    private fun dispatchPreferenceResult(success: Boolean, enrollmentOpened: Boolean) {
        val script = """
            window.dispatchEvent(new CustomEvent('nationNumsBiometricPreferenceResult', {
              detail: { success: $success, enrollmentOpened: $enrollmentOpened }
            }));
        """.trimIndent()
        webView.post { webView.evaluateJavascript(script, null) }
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
