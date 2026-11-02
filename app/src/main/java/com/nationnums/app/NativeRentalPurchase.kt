package com.nationnums.app

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import kotlinx.coroutines.launch

@Composable
internal fun NativeRentalPurchase(backend: Base44Api, modifier: Modifier, onBack: () -> Unit, onPurchased: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val pick = remember { NativeRentalSelection() }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    suspend fun load() {
        val payload = pick.payload()
        when (pick.stage) { 1 -> payload.put("durations", true); 2 -> payload.put("catalog", true); 3 -> payload.put("list", true) }
        state.call(if (pick.stage == 0) "listEligibleServers" else "getPurchasePrice", payload)
    }
    LaunchedEffect(pick.stage) { load() }
    NativeBackendPage(listOf("Choose rental server", "Choose rental duration", "Choose rental country", "Choose rental service", "Review rental")[pick.stage], state, modifier, {
        if (!pick.back()) onBack() else search = ""
    }, { scope.launch { load() } }) {
        val entries = state.value.rows(listOf("servers", "durations", "countries", "services", "none")[pick.stage])
        val visibleEntries = entries.filter { entry ->
            val label = when (pick.stage) {
                0 -> entry.text("name")
                1 -> entry.text("label")
                2 -> entry.text("country") + " " + entry.text("country_code")
                else -> entry.text("service_name") + " " + entry.text("service")
            }
            label.contains(search.trim(), ignoreCase = true)
        }
        if (pick.stage < 4) item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(when (pick.stage) { 0 -> "Search servers"; 1 -> "Search durations"; 2 -> "Search countries"; else -> "Search services" }) },
                singleLine = true,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (pick.stage < 4 && !state.busy && visibleEntries.isEmpty() && state.error.isBlank()) item {
            Text(if (entries.isEmpty()) "No rental options are available for this selection." else "No options match your search.")
        }
        items(visibleEntries) { entry ->
            OutlinedButton(enabled = !state.busy, onClick = { pick.choose(entry); search = "" }) {
                Text(when (pick.stage) { 0 -> entry.text("name"); 1 -> entry.text("label"); 2 -> entry.text("country"); else -> entry.text("service_name").ifBlank { entry.text("service") } })
            }
        }
        if (pick.stage == 4 && !state.busy && state.error.isBlank()) item {
            Text(pick.country?.text("country").orEmpty() + " · " + pick.service?.text("service_name").orEmpty() + " · " + pick.duration?.text("label").orEmpty())
            if (state.value.optDouble("customer_price", 0.0) > 0) Button(onClick = { confirm = true }) { Text("Rent for ₦" + state.value.text("customer_price")) }
            else Text("This rental has no confirmed price yet. Choose another selection; your wallet has not been charged.")
        }
    }
    if (confirm) {
        val price = remember { state.value.optDouble("customer_price", 0.0) }
        NativePurchaseConfirm(price.toString(), pick.service?.text("service_name").orEmpty() + " · " + pick.duration?.text("label").orEmpty(), state.busy, state.error, { if (!state.busy) { confirm = false; scope.launch { load() } } }) {
            scope.launch { state.call("provisionNumber", pick.payload().put("expected_price", price))?.let { if (it.optBoolean("success")) { confirm = false; onPurchased() } else state.error = "Rental was not completed." } }
        }
    }
}
