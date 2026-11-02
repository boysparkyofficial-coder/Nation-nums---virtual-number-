package com.nationnums.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun NativeProxyPurchase(
    backend: Base44Api,
    modifier: Modifier,
    onBack: () -> Unit,
    onPurchased: () -> Unit,
    onOrders: () -> Unit
) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var selectedOffer by remember { mutableStateOf<JSONObject?>(null) }
    var country by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var quote by remember { mutableStateOf<JSONObject?>(null) }
    var priceChange by remember { mutableStateOf<JSONObject?>(null) }
    var typeFilter by remember { mutableStateOf("all") }
    var countryFilter by remember { mutableStateOf("") }
    var protocolFilter by remember { mutableStateOf("") }
    var ipFilter by remember { mutableStateOf("") }
    var rotationFilter by remember { mutableStateOf("") }
    var sessionFilter by remember { mutableStateOf("") }
    var billingFilter by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var filtersExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { state.call("getProxyCatalog") }

    val offers = state.value.rows("offers")
    val unavailable = state.value.rows("unavailable")
    val facets = state.value.optJSONObject("facets") ?: JSONObject()
    val typeOptions = facets.rows("types")
    val countryOptions = facets.rows("countries").map { it.text("code") to it.text("name") }
    val protocolOptions = facets.rows("protocols").map { it.text("key") to it.text("label") }
    val ipOptions = facets.rows("ip_versions").map { it.text("key") to it.text("label") }
    val rotationOptions = facets.rows("rotation_modes").map { it.text("key") to it.text("label") }
    val billingOptions = facets.rows("billing_models").map { it.text("key") to it.text("label") }
    val sessionOptions = facets.stringValues("sessions").map { it to it }

    val visibleOffers = offers.filter { offer ->
        val priceLimit = maxPrice.toDoubleOrNull()
        (typeFilter == "all" || offer.text("proxy_type") == typeFilter) &&
            (countryFilter.isBlank() || offer.rows("countries").any { it.text("code") == countryFilter }) &&
            (protocolFilter.isBlank() || offer.hasString("protocols", protocolFilter)) &&
            (ipFilter.isBlank() || offer.hasString("ip_versions", ipFilter)) &&
            (rotationFilter.isBlank() || offer.hasString("rotation_modes", rotationFilter)) &&
            (sessionFilter.isBlank() || offer.text("session") == sessionFilter) &&
            (billingFilter.isBlank() || offer.text("billing_model") == billingFilter) &&
            (priceLimit == null || priceLimit <= 0 || offer.optDouble("price_per_unit_ngn", 0.0) <= priceLimit)
    }

    NativeBackendPage(
        "Proxy services",
        state,
        modifier,
        {
            if (quote != null) {
                quote = null
                priceChange = null
            } else if (selectedOffer != null) {
                selectedOffer = null
                country = ""
                quantity = "1"
                scope.launch { state.call("getProxyCatalog") }
            } else onBack()
        },
        { scope.launch { state.call("getProxyCatalog") } }
    ) {
        item {
            TextButton(onClick = onOrders, enabled = !state.busy) { Text("My proxy orders and usage") }
        }

        if (selectedOffer == null) {
            item {
                Text("Choose a proxy type", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = { typeFilter = "all" }) { Text(if (typeFilter == "all") "✓ All" else "All") }
                    typeOptions.forEach { type ->
                        val key = type.text("key")
                        val label = type.text("short").ifBlank { type.text("label") }
                        val count = type.optInt("count")
                        TextButton(onClick = { typeFilter = key }) {
                            Text((if (typeFilter == key) "✓ " else "") + label + if (count == 0) " · Soon" else " · $count")
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Filters", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                        Text(if (filtersExpanded) "Hide" else "Show")
                    }
                    TextButton(onClick = {
                        countryFilter = ""; protocolFilter = ""; ipFilter = ""
                        rotationFilter = ""; sessionFilter = ""; billingFilter = ""; maxPrice = ""
                    }) { Text("Clear") }
                }
                if (filtersExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NativeProxyFilter("Country", countryFilter, listOf("" to "All countries") + countryOptions) { countryFilter = it }
                        NativeProxyFilter("Protocol", protocolFilter, listOf("" to "Any") + protocolOptions) { protocolFilter = it }
                        NativeProxyFilter("IP version", ipFilter, listOf("" to "Any") + ipOptions) { ipFilter = it }
                        NativeProxyFilter("Rotation", rotationFilter, listOf("" to "Any") + rotationOptions) { rotationFilter = it }
                        if (sessionOptions.isNotEmpty()) NativeProxyFilter("Session / duration", sessionFilter, listOf("" to "Any") + sessionOptions) { sessionFilter = it }
                        NativeProxyFilter("Billing", billingFilter, listOf("" to "Any") + billingOptions) { billingFilter = it }
                        OutlinedTextField(
                            value = maxPrice,
                            onValueChange = { maxPrice = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                            label = { Text("Maximum price per unit (₦)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (!state.busy && offers.isEmpty() && state.error.isBlank()) {
                item { Text(state.value.text("message").ifBlank { "No verified proxy products are available right now." }) }
                unavailable.forEach { row ->
                    item { Text(row.text("label") + " — " + row.text("message"), style = MaterialTheme.typography.bodySmall) }
                }
            } else if (!state.busy && visibleOffers.isEmpty() && state.error.isBlank()) {
                item { Text("No products match these filters. Clear the filters or choose another proxy type.") }
            }
            items(visibleOffers) { offer ->
                OutlinedButton(
                    onClick = { selectedOffer = offer; country = ""; quantity = "1" },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(offer.text("type_label").ifBlank { offer.text("proxy_type") }, style = MaterialTheme.typography.titleSmall)
                        Text("₦" + offer.text("price_per_unit_ngn") + " / " + offer.text("unit_label").ifBlank { "GB" }, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            listOf(
                                offer.text("billing_label"),
                                offer.rows("protocols").joinToString { it.text("label").ifBlank { it.text("key") } },
                                offer.rows("rotation_modes").joinToString { it.text("label").ifBlank { it.text("key") } },
                                offer.rows("ip_versions").joinToString { it.text("label").ifBlank { it.text("key") } },
                                offer.text("session")
                            ).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("${offer.rows("countries").size} countries available", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            val currentOffer = selectedOffer!!
            item { Text(currentOffer.text("type_label").ifBlank { currentOffer.text("proxy_type") }, style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter(Char::isDigit).take(3) },
                    label = { Text("Data quantity in GB · 1–500") },
                    singleLine = true,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Choose an exit country (optional)")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = { country = "" }, enabled = !state.busy) {
                        Text(if (country.isBlank()) "✓ Any country" else "Any country")
                    }
                    currentOffer.rows("countries").forEach { option ->
                        val code = option.text("code")
                        TextButton(onClick = { country = code }, enabled = !state.busy) {
                            Text((if (country == code) "✓ " else "") + option.text("name"))
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 5, 10, 25).forEach { preset ->
                        OutlinedButton(onClick = { quantity = preset.toString() }, enabled = !state.busy) { Text("$preset GB") }
                    }
                }
            }
            item {
                Button(
                    enabled = !state.busy && (quantity.toIntOrNull() ?: 0) in 1..500,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val units = quantity.toIntOrNull() ?: 0
                        val payload = JSONObject()
                            .put("offer_key", currentOffer.text("offer_key"))
                            .put("quantity", units)
                        if (country.isNotBlank()) payload.put("country_code", country)
                        scope.launch {
                            state.call("getProxyQuote", payload)?.let { root ->
                                quote = root.optJSONObject("quote")
                                priceChange = null
                            }
                        }
                    }
                ) { Text(if (state.busy) "Checking live price…" else "Get current price") }
            }
            item { Text("The supplier price is checked again before your wallet is charged.", style = MaterialTheme.typography.bodySmall) }
        }
    }

    val currentQuote = quote
    if (currentQuote != null && priceChange == null) {
        NativePurchaseConfirm(
            currentQuote.text("price_ngn"),
            currentQuote.text("proxy_type") + " · " + currentQuote.text("quantity") + " GB",
            state.busy,
            state.error,
            { if (!state.busy) quote = null }
        ) {
            scope.launch {
                val result = state.call("provisionProxy", JSONObject().put("quote_token", currentQuote.text("token")))
                if (result?.optBoolean("success") == true) {
                    quote = null
                    onPurchased()
                } else if (state.errorCode == "price_changed") {
                    priceChange = state.errorData
                }
            }
        }
    }
    if (currentQuote != null && priceChange != null) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) { quote = null; priceChange = null } },
            title = { Text("Proxy price changed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The live supplier price changed. Nothing has been charged yet.")
                    Text("Previous price: ₦" + priceChange!!.opt("previous_price").toString())
                    Text("New price: ₦" + priceChange!!.opt("new_price").toString())
                }
            },
            confirmButton = {
                Button(enabled = !state.busy, onClick = {
                    scope.launch {
                        val result = state.call(
                            "provisionProxy",
                            JSONObject().put("quote_token", currentQuote.text("token")).put("confirm_price_change", true)
                        )
                        if (result?.optBoolean("success") == true) {
                            quote = null
                            priceChange = null
                            onPurchased()
                        } else if (state.errorCode == "price_changed") {
                            priceChange = state.errorData
                        }
                    }
                }) { Text(if (state.busy) "Processing…" else "Accept new price") }
            },
            dismissButton = {
                TextButton(enabled = !state.busy, onClick = { quote = null; priceChange = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NativeProxyFilter(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember(label) { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "Any"
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedLabel)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = { onSelected(option.first); expanded = false }
                    )
                }
            }
        }
    }
}

private fun JSONObject.stringValues(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
}

private fun JSONObject.hasString(key: String, value: String): Boolean =
    stringValues(key).contains(value)
