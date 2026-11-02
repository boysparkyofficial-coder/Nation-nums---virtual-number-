package com.nationnums.app

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeSupportPage(backend: Base44Api, modifier: Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var tickets by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var cursor by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var create by remember { mutableStateOf(false) }
    suspend fun load(more: Boolean = false) {
        state.call("mobileSupport", JSONObject().put("action", "list").apply { if (more) put("cursor", cursor) })?.let { root -> tickets = (if (more) tickets else emptyList()) + root.rows("items"); cursor = if (root.optBoolean("has_more")) root.text("next_cursor") else "" }
    }
    LaunchedEffect(Unit) { load() }
    if (selected != null) { NativeSupportThread(backend, selected!!, modifier) { selected = null; scope.launch { load() } }; return }
    NativeBackendPage("Help & support", state, modifier, onBack, { scope.launch { load() } }) {
        item { Button(onClick = { create = true }, enabled = !state.busy) { Text("New support ticket") }; TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:nationnums@gmail.com?subject=Nation%20Nums%20Support"))) }.onFailure { state.error = "No email app could open support. Please use a support ticket." } }) { Text("Email customer support") } }
        if (!state.busy && tickets.isEmpty() && state.error.isBlank()) item { Text("No support tickets yet. Open a ticket to contact our team.") }
        items(tickets, key = { it.text("id") }) { ticket -> OutlinedButton(onClick = { selected = ticket }, enabled = !state.busy) { Text(ticket.text("subject") + " · " + ticket.text("status")) } }
        if (cursor.isNotBlank()) item { OutlinedButton(enabled = !state.busy, onClick = { scope.launch { load(true) } }) { Text("Load more tickets") } }
    }
    if (create) NativeSupportCreate(state, { create = false }) { subject, category, message -> scope.launch {
        state.call("mobileSupport", JSONObject().put("action", "create").put("subject", subject).put("category", category).put("message", message))?.let { root -> create = false; selected = root.optJSONObject("ticket"); load() }
    } }
}
