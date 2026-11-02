package com.nationnums.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeRecordHistory(backend: Base44Api, social: Boolean, modifier: Modifier, onBack: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var records by remember(social) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var cursor by remember(social) { mutableStateOf("") }
    suspend fun load(more: Boolean = false) {
        val payload = JSONObject().put("section", if (social) "social_orders" else "proxy_orders")
        if (more) payload.put("cursor", cursor)
        state.call("mobileAccount", payload)?.let { root -> records = (if (more) records else emptyList()) + root.rows("items"); cursor = if (root.optBoolean("has_more")) root.text("next_cursor") else "" }
    }
    LaunchedEffect(social) { load() }
    NativeBackendPage(if (social) "Social account orders" else "My proxies", state, modifier, onBack, { scope.launch { load() } }) {
        if (!state.busy && records.isEmpty() && state.error.isBlank()) item { Text("No orders yet.") }
        items(records, key = { it.text("id") }) { record -> NativeDeliveredOrder(backend, record, social) }
        if (cursor.isNotBlank()) item { OutlinedButton(onClick = { scope.launch { load(true) } }, enabled = !state.busy) { Text("Load more orders") } }
    }
}
