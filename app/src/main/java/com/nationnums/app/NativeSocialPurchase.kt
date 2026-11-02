package com.nationnums.app

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeSocialPurchase(backend: Base44Api, modifier: Modifier, onBack: () -> Unit, onPurchased: () -> Unit, onOrders: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var records by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    suspend fun load(more: Boolean = false) {
        val next = if (more) page + 1 else 1
        state.call("getSocialLogsCatalog", JSONObject().put("search", search.trim()).put("page", next))?.let { root -> page = next; records = (if (more) records else emptyList()) + root.rows("listings") }
    }
    LaunchedEffect(Unit) { load() }
    NativeBackendPage("Social media accounts", state, modifier, onBack, { scope.launch { load() } }) {
        item { TextButton(onClick = onOrders, enabled = !state.busy) { Text("My delivered accounts") }; OutlinedTextField(search, { search = it }, label = { Text("Search accounts") }, singleLine = true); Button(enabled = !state.busy, onClick = { scope.launch { load() } }) { Text("Search") } }
        if (!state.busy && records.isEmpty() && state.error.isBlank()) item { Text("No matching listings are available.") }
        items(records, key = { it.text("slug") }) { listing ->
            OutlinedButton(enabled = !state.busy && listing.optBoolean("purchasable"), onClick = { selected = listing }) { Text(listing.text("title") + " · ₦" + listing.text("unit_price") + " · " + listing.text("stock") + " in stock") }
        }
        if (page < state.value.optInt("last_page", 1)) item { OutlinedButton(enabled = !state.busy, onClick = { scope.launch { load(true) } }) { Text("Load more listings") } }
    }
    selected?.let { listing -> NativeSocialConfirm(listing, state, { if (!state.busy) selected = null }) { units -> scope.launch {
        state.call("provisionSocialLog", JSONObject().put("slug", listing.text("slug")).put("quantity", units))?.let { if (it.optBoolean("success")) { selected = null; onPurchased() } else state.error = "Purchase was not completed." }
    } } }
}
