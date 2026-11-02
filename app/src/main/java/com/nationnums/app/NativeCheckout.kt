package com.nationnums.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeCheckout(backend: Base44Api, onChanged: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var checkoutUrl by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun openCheckout() { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))) }.onFailure { message = "No browser could open the payment page." } }
    Column {
        Text("NationCheckout", style = MaterialTheme.typography.titleMedium)
        if (reference.isBlank()) {
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount in NGN · minimum ₦500") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy, onClick = {
                val value = amount.toDoubleOrNull()
                if (value == null || !value.isFinite() || value < 500 || value > 2000000) message = "Enter an amount between ₦500 and ₦2,000,000."
                else scope.launch {
                    busy = true; message = ""
                    val result = backend.awaitFunction("createCheckoutPayment", JSONObject().put("amount", value))
                    if (result.success) {
                        val json = JSONObject(result.body); val url = json.optString("checkout_url"); val ref = json.optString("reference")
                        if (Uri.parse(url).scheme == "https" && !Uri.parse(url).host.isNullOrBlank() && ref.isNotBlank()) { reference = ref; checkoutUrl = url; openCheckout() } else message = "The payment provider did not return a valid checkout."
                    } else message = result.error ?: "Could not start payment."
                    busy = false
                }
            }) { Text(if (busy) "Starting checkout…" else "Fund wallet") }
        } else {
            Text("Reference: " + reference)
            TextButton(onClick = { openCheckout() }, enabled = !busy) { Text("Reopen payment page") }
            Button(enabled = !busy, onClick = { scope.launch {
                busy = true; message = ""
                val result = backend.awaitFunction("verifyCheckoutPayment", JSONObject().put("reference", reference))
                if (result.success) { val status = JSONObject(result.body).optString("payment_status"); if (status == "completed" || status == "held") { message = if (status == "held") "Payment received and held according to the wallet minimum." else "Payment confirmed. Your wallet has been refreshed."; reference = ""; checkoutUrl = ""; onChanged() } else message = "Payment is not confirmed yet. You can check again after paying." } else message = result.error ?: "Could not check payment."
                busy = false
            } }) { Text(if (busy) "Verifying…" else "I have paid — check payment") }
        }
        if (message.isNotBlank()) Text(message)
        Text("Only payment-provider confirmation can credit your wallet.", style = MaterialTheme.typography.bodySmall)
    }
}
