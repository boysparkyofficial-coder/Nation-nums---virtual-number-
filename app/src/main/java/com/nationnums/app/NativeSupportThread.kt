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
internal fun NativeSupportThread(backend: Base44Api, ticket: JSONObject, modifier: Modifier, onBack: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var replies by remember(ticket.text("id")) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var cursor by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var closed by remember { mutableStateOf(ticket.text("status") == "closed") }
    suspend fun load(more: Boolean = false) {
        state.call("mobileSupport", JSONObject().put("action", "replies").put("ticket_id", ticket.text("id")).apply { if (more) put("cursor", cursor) })?.let { root -> replies = (if (more) replies else emptyList()) + root.rows("items"); cursor = if (root.optBoolean("has_more")) root.text("next_cursor") else ""; closed = root.optJSONObject("ticket")?.text("status") == "closed" }
    }
    LaunchedEffect(ticket.text("id")) { load() }
    NativeBackendPage(ticket.text("subject"), state, modifier, onBack, { scope.launch { load() } }) {
        items(replies, key = { it.text("id") }) { reply -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(if (reply.optBoolean("by_admin")) "Support" else "You", style = MaterialTheme.typography.labelMedium); Text(reply.text("body")); Text(reply.text("created_date"), style = MaterialTheme.typography.labelSmall) } } }
        if (cursor.isNotBlank()) item { OutlinedButton(enabled = !state.busy, onClick = { scope.launch { load(true) } }) { Text("Load more replies") } }
        if (closed) item { Text("This ticket is closed. Open a new ticket for more help.") }
        else item {
            OutlinedTextField(message, { message = it.take(5000) }, label = { Text("Your reply") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, enabled = !state.busy)
            Button(enabled = !state.busy && message.isNotBlank(), onClick = { scope.launch {
                state.call("mobileSupport", JSONObject().put("action", "reply").put("ticket_id", ticket.text("id")).put("message", message.trim()))?.let { message = ""; load() }
            } }) { Text(if (state.busy) "Sending…" else "Send reply") }
        }
    }
}
