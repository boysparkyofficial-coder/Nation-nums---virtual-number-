package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeNumberActions(backend: Base44Api, number: VirtualNumberRecord, onChanged: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var cancel by remember(number.id) { mutableStateOf(false) }
    var message by remember(number.id) { mutableStateOf("") }
    if (number.status.equals("active", true) || number.status.equals("standby", true)) Column {
        OutlinedButton(enabled = !state.busy, onClick = { scope.launch {
            state.call("fetchInbox", JSONObject().put("virtual_number_id", number.id))?.let { root -> val received = root.rows("new_messages"); message = if (received.isEmpty()) "Inbox refreshed. Received messages appear in your orders below." else received.joinToString("\n") { it.text("otp_code") + " " + it.text("body") }; onChanged() }
        } }) { Text(if (state.busy) "Please wait…" else "Check for SMS code") }
        TextButton(enabled = !state.busy, onClick = { cancel = true }) { Text("Cancel unused number") }
        if (message.isNotBlank()) Text(message)
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
    }
    if (cancel) AlertDialog(onDismissRequest = { if (!state.busy) cancel = false }, title = { Text("Cancel this number?") }, text = { Column { Text("The server allows cancellation only when eligible and before a code has been received. Any approved refund goes back to your Nation Nums wallet."); if (state.error.isNotBlank()) Text(state.error) } }, confirmButton = { Button(enabled = !state.busy, onClick = { scope.launch {
        state.call("cancelNumber", JSONObject().put("virtual_number_id", number.id))?.let { cancel = false; onChanged() }
    } }) { Text(if (state.busy) "Cancelling…" else "Confirm cancellation") } }, dismissButton = { TextButton(onClick = { cancel = false }, enabled = !state.busy) { Text("Keep number") } })
}
