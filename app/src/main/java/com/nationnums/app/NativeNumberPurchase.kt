package com.nationnums.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeNumberPurchase(backend: Base44Api, usOnly: Boolean, modifier: Modifier, onBack: () -> Unit, onPurchased: () -> Unit) {
    var country by remember(usOnly) { mutableStateOf<JSONObject?>(if (usOnly) JSONObject().put("country_code", "US").put("country", "United States") else null) }
    var service by remember(usOnly) { mutableStateOf("") }
    var options by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf<JSONObject?>(null) }
    var search by remember(usOnly) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val serviceKey = if (usOnly) "us_number" else "sms_otp"
    fun selection() = JSONObject().put("country_code", country?.optString("country_code")).put("country", country?.optString("country")).put("flag", country?.optString("flag")).put("service", service).put("service_key", serviceKey).put("plan_type", "temporary")
    LaunchedEffect(country?.optString("country_code"), service) {
        busy = true; error = ""; options = emptyList()
        val payload = selection().put("action", if (country == null) "countries" else "services")
        val result = backend.awaitFunction(if (service.isBlank()) "getSmsCatalog" else "getSmsOffers", payload)
        if (result.success) { val root = JSONObject(result.body); val key = if (country == null) "countries" else if (service.isBlank()) "services" else "offers"; options = Base44RecordParser.records(JSONObject().put("items", root.optJSONArray(key)).toString()) }
        else error = result.error ?: "Could not load this selection."
        busy = false
    }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            TextButton(onClick = {
                when {
                    service.isNotBlank() -> { service = ""; search = "" }
                    country != null && !usOnly -> { country = null; search = "" }
                    else -> onBack()
                }
            }, enabled = !busy) { Text("Back") }
            Text(
                if (country == null) "Choose a country" else if (service.isBlank()) country!!.optString("country") + " services" else service + " — live offers",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        if (country == null || service.isBlank()) item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(if (country == null) "Search countries" else "Search services") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = { onBack() }) { Text("Return and try again") } }
        if (!busy && error.isBlank() && options.isEmpty()) item { Text("No services are available for this selection right now.") }
        val visibleOptions = options.filter { option ->
            val searchable = when {
                country == null -> listOf(option.optString("country"), option.optString("country_code"), option.optString("name"))
                service.isBlank() -> listOf(option.optString("service"), option.optString("label"), option.optString("name"))
                else -> listOf(option.optString("label"), option.optString("server_name"), option.optString("price"))
            }.joinToString(" ")
            searchable.contains(search.trim(), ignoreCase = true)
        }
        if (!busy && error.isBlank() && visibleOptions.isEmpty() && options.isNotEmpty()) item {
            Text(if (country == null) "No countries match your search." else "No services match your search.")
        }
        items(visibleOptions) { option ->
            val available = service.isBlank() || (option.optBoolean("available") && option.optDouble("price", 0.0) > 0)
            OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && available, onClick = {
                when {
                    country == null -> { country = option; search = "" }
                    service.isBlank() -> { service = option.optString("service"); search = "" }
                    else -> scope.launch {
                        busy = true; error = ""
                        val result = backend.awaitFunction("getPurchasePrice", selection().put("server_id", option.optString("server_id")))
                        if (result.success) quote = JSONObject(result.body) else error = result.error ?: "Unable to confirm the live price."
                        busy = false
                    }
                }
            }) { Text(if (country == null) option.optString("flag") + " " + option.optString("country") else if (service.isBlank()) option.optString("service") else option.optString("label") + if (available) " · ₦" + option.optString("price") else " · Unavailable") }
        }
    }
    quote?.let { current ->
        NativePurchaseConfirm(current.optString("customer_price"), service, busy, error, onDismiss = { if (!busy) { quote = null; error = "" } }) {
            scope.launch {
                busy = true; error = ""
                val result = backend.awaitFunction("provisionNumber", selection().put("server_id", current.optString("server_id")).put("expected_price", current.optDouble("customer_price")))
                if (result.success && JSONObject(result.body).optBoolean("success")) { quote = null; onPurchased() }
                else { error = result.error ?: "Purchase was not completed."; val updated = runCatching { JSONObject(result.body).optDouble("customer_price", 0.0) }.getOrDefault(0.0); if (updated > 0) quote = JSONObject(current.toString()).put("customer_price", updated) }
                busy = false
            }
        }
    }
}
