package com.nationnums.app

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Dialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeCheckout(backend: Base44Api, onChanged: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var checkoutUrl by rememberSaveable { mutableStateOf("") }
    var browserOpen by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = browserOpen) { browserOpen = false }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("NationCheckout", style = MaterialTheme.typography.titleMedium)
            Text("Fund your Nation Nums wallet securely without leaving the app.", style = MaterialTheme.typography.bodySmall)
            if (reference.isBlank()) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount in NGN · minimum ₦500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(enabled = !busy, onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value == null || !value.isFinite() || value < 500 || value > 2000000) {
                        message = "Enter an amount between ₦500 and ₦2,000,000."
                    } else scope.launch {
                        busy = true
                        message = ""
                        val result = backend.awaitFunction("createCheckoutPayment", JSONObject().put("amount", value))
                        if (result.success) {
                            val json = JSONObject(result.body)
                            val url = json.optString("checkout_url")
                            val ref = json.optString("reference")
                            val parsed = android.net.Uri.parse(url)
                            if (parsed.scheme.equals("https", true) && !parsed.host.isNullOrBlank() && ref.isNotBlank()) {
                                reference = ref
                                checkoutUrl = url
                                browserOpen = true
                            } else message = "NationCheckout did not return a valid secure payment page."
                        } else message = result.error ?: "Could not start NationCheckout."
                        busy = false
                    }
                }) { Text(if (busy) "Starting checkout…" else "Fund wallet with NationCheckout") }
            } else {
                Text("Payment reference: $reference", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { browserOpen = true }, enabled = !busy && checkoutUrl.isNotBlank()) {
                    Text("Continue payment inside the app")
                }
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        message = ""
                        val result = backend.awaitFunction("verifyCheckoutPayment", JSONObject().put("reference", reference))
                        if (result.success) {
                            val status = JSONObject(result.body).optString("payment_status")
                            if (status == "completed" || status == "held") {
                                message = if (status == "held") {
                                    "Payment received and held according to the wallet minimum."
                                } else {
                                    "Payment confirmed. Your wallet has been refreshed."
                                }
                                reference = ""
                                checkoutUrl = ""
                                amount = ""
                                onChanged()
                            } else message = "Payment is not confirmed yet. Finish payment, then check again."
                        } else message = result.error ?: "Could not verify this NationCheckout payment."
                        busy = false
                    }
                }) { Text(if (busy) "Verifying…" else "I have paid — verify payment") }
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            Text("Only verified payment-provider confirmation can credit your wallet.", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (browserOpen && checkoutUrl.isNotBlank()) {
        Dialog(
            onDismissRequest = { browserOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NationCheckout", style = MaterialTheme.typography.titleMedium)
                            Text("Secure payment · stays inside Nation Nums", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { browserOpen = false }) { Text("Close") }
                    }
                    Spacer(Modifier.height(1.dp))
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val scheme = request?.url?.scheme
                                        // Keep secure HTTPS payment redirects inside this in-app browser.
                                        // Block intent:, file:, content:, javascript: and other non-web schemes.
                                        return !scheme.equals("https", ignoreCase = true)
                                    }

                                    @Suppress("DEPRECATION")
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        return !android.net.Uri.parse(url.orEmpty()).scheme.equals("https", ignoreCase = true)
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                loadUrl(checkoutUrl)
                            }
                        },
                        update = { view ->
                            if (view.tag != checkoutUrl) {
                                view.tag = checkoutUrl
                                view.loadUrl(checkoutUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}
