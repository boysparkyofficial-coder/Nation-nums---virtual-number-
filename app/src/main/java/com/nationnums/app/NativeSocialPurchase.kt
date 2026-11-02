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
import org.json.JSONObject
import java.util.UUID

@Composable
internal fun NativeSocialPurchase(
    backend: Base44Api,
    modifier: Modifier,
    onBack: () -> Unit,
    onPurchased: () -> Unit,
    onOrders: () -> Unit
) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var categoryId by remember { mutableStateOf(0) }
    var records by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var requestSelection by remember { mutableStateOf("") }
    var requestId by remember { mutableStateOf("") }

    suspend fun load(more: Boolean = false, category: Int = categoryId) {
        val next = if (more) page + 1 else 1
        val payload = JSONObject().put("search", search.trim()).put("page", next)
        if (category > 0) payload.put("category_id", category)
        state.call("getSocialLogsCatalog", payload)?.let { root ->
            page = root.optInt("page", next)
            records = (if (more) records else emptyList()) + root.rows("listings")
        }
    }

    LaunchedEffect(Unit) { load() }

    NativeBackendPage(
        "Social media logs",
        state,
        modifier,
        onBack,
        { scope.launch { load() } }
    ) {
        item {
            TextButton(onClick = onOrders, enabled = !state.busy) { Text("My orders") }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search Instagram, TikTok, Gmail…") },
                singleLine = true,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { scope.launch { load() } },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Search listings") }
        }
        item {
            Text("Categories", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { categoryId = 0; scope.launch { load(category = 0) } }, enabled = !state.busy) {
                    Text(if (categoryId == 0) "✓ All" else "All")
                }
                state.value.rows("categories").forEach { category ->
                    val id = category.optInt("id")
                    TextButton(onClick = { categoryId = id; scope.launch { load(category = id) } }, enabled = !state.busy) {
                        Text((if (categoryId == id) "✓ " else "") + category.text("title"))
                    }
                }
            }
        }
        if (!state.busy && records.isEmpty() && state.error.isBlank()) {
            item { Text("No matching social media listings are available.") }
        }
        items(records, key = { it.text("slug") }) { listing ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (listing.text("category").isNotBlank()) {
                        Text(listing.text("category").uppercase(), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(listing.text("title"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        "₦" + listing.text("unit_price") + " each · " +
                            (if (listing.optInt("stock") > 0) listing.text("stock") + " in stock" else "Out of stock") +
                            (if (listing.optInt("sold") > 0) " · " + listing.text("sold") + " sold" else ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { selected = listing },
                        enabled = !state.busy && listing.optBoolean("purchasable") && listing.optInt("stock") > 0
                    ) { Text(if (listing.optInt("stock") <= 0) "Sold out" else if (listing.optBoolean("purchasable")) "Buy" else "Unavailable") }
                }
            }
        }
        if (page < state.value.optInt("last_page", 1)) {
            item {
                OutlinedButton(onClick = { scope.launch { load(more = true) } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Load more listings")
                }
            }
        }
    }

    selected?.let { listing ->
        NativeSocialConfirm(listing, state, { if (!state.busy) selected = null }) { units ->
            scope.launch {
                val selection = listing.text("slug") + ":" + units
                if (requestSelection != selection || requestId.isBlank()) {
                    requestSelection = selection
                    requestId = UUID.randomUUID().toString()
                }
                val expectedTotal = listing.optDouble("unit_price", 0.0) * units
                val result = state.call(
                    "provisionSocialLog",
                    JSONObject()
                        .put("slug", listing.text("slug"))
                        .put("quantity", units)
                        .put("request_id", requestId)
                        .put("expected_price", expectedTotal)
                )
                if (result?.optBoolean("success") == true) {
                    selected = null
                    requestSelection = ""
                    requestId = ""
                    onPurchased()
                }
            }
        }
    }
}
