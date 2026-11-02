package com.nationnums.app

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeEmailPurchase(backend: Base44Api, modifier: Modifier, onBack: () -> Unit, onPurchased: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var site by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf<JSONObject?>(null) }
    suspend fun load() { state.call("getEmailOtpCatalog", JSONObject().apply { if (site.isNotBlank()) put("site", site) }) }
    LaunchedEffect(site) { load() }
    NativeBackendPage(if (site.isBlank()) "Email verification" else site + " email domains", state, modifier, { if (site.isNotBlank()) { site = ""; search = "" } else onBack() }, { scope.launch { load() } }) {
        item { OutlinedTextField(search, { search = it }, label = { Text("Search platforms or domains") }, singleLine = true) }
        val entries = state.value.rows(if (site.isBlank()) "products" else "domains")
        val visible = entries.filter { (it.text("label") + it.text("site") + it.text("domain")).contains(search, true) }
        if (!state.busy && visible.isEmpty() && state.error.isBlank()) item { Text("No email services available for this selection.") }
        items(visible) { entry ->
            OutlinedButton(enabled = !state.busy, onClick = { if (site.isBlank()) { site = entry.text("site"); search = "" } else selection = entry }) {
                Text(if (site.isBlank()) entry.text("label").ifBlank { entry.text("site") } else entry.text("domain") + " · ₦" + entry.text("customer_price"))
            }
        }
    }
    selection?.let { domain -> NativePurchaseConfirm(domain.text("customer_price"), site + " · " + domain.text("domain"), state.busy, state.error, { if (!state.busy) selection = null }) {
        scope.launch {
            val payload = JSONObject().put("service_key", "email_otp").put("service", site).put("site", site).put("domain", domain.text("domain")).put("plan_type", "temporary")
            state.call("provisionNumber", payload)?.let { if (it.optBoolean("success")) { selection = null; onPurchased() } else state.error = "Purchase was not completed." }
        }
    } }
}
