package com.nationnums.app

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeProxyPurchase(backend: Base44Api, modifier: Modifier, onBack: () -> Unit, onPurchased: () -> Unit, onOrders: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<JSONObject?>(null) }
    var country by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var quote by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(Unit) { state.call("getProxyCatalog") }
    NativeBackendPage("Proxy services", state, modifier, { if (offer != null) { offer = null; country = ""; scope.launch { state.call("getProxyCatalog") } } else onBack() }, { scope.launch { state.call("getProxyCatalog") } }) {
        item { TextButton(onClick = onOrders, enabled = !state.busy) { Text("My proxy orders") } }
        if (offer == null) {
            val offers = state.value.rows("offers")
            if (!state.busy && offers.isEmpty() && state.error.isBlank()) item { Text(state.value.text("message").ifBlank { "No proxy products are available right now." }) }
            items(offers) { entry -> OutlinedButton(onClick = { offer = entry }, enabled = !state.busy) { Text(entry.text("title").ifBlank { entry.text("proxy_type") } + " · ₦" + entry.text("price_per_unit_ngn") + "/GB") } }
        } else {
            item { OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity in GB · 1–500") }, singleLine = true, enabled = !state.busy) }
            item { Text("Choose an exit country") }
            items(offer!!.rows("countries")) { entry -> OutlinedButton(onClick = { country = entry.text("code") }, enabled = !state.busy) { Text((if (country == entry.text("code")) "✓ " else "") + entry.text("name")) } }
            item { Button(enabled = !state.busy && country.isNotBlank(), onClick = { scope.launch {
                val units = quantity.toIntOrNull()
                if (units == null || units !in 1..500) state.error = "Enter a whole quantity between 1 and 500."
                else state.call("getProxyQuote", JSONObject().put("offer_key", offer!!.text("offer_key")).put("country_code", country).put("quantity", units))?.let { quote = it.optJSONObject("quote") }
            } }) { Text("Get current price") } }
        }
    }
    quote?.let { current -> NativePurchaseConfirm(current.text("price_ngn"), current.text("proxy_type") + " · " + current.text("quantity") + " GB", state.busy, state.error, { if (!state.busy) quote = null }) {
        scope.launch { val result = state.call("provisionProxy", JSONObject().put("quote_token", current.text("token"))); quote = null; if (result?.optBoolean("success") == true) onPurchased() }
    } }
}
