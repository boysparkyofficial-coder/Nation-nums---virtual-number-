package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
internal fun NativeSupportCreate(state: NativeBackendState, onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("other") }
    AlertDialog(onDismissRequest = { if (!state.busy) onDismiss() }, title = { Text("New support ticket") }, text = { Column {
        OutlinedTextField(subject, { subject = it.take(150) }, label = { Text("Subject") }, enabled = !state.busy)
        TextButton(enabled = !state.busy, onClick = { category = when (category) { "other" -> "account"; "account" -> "payment"; "payment" -> "number"; "number" -> "service"; else -> "other" } }) { Text("Category: " + category + " · tap to change") }
        OutlinedTextField(message, { message = it.take(5000) }, label = { Text("Message") }, minLines = 3, maxLines = 6, enabled = !state.busy)
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
    } }, confirmButton = { Button(enabled = !state.busy && subject.isNotBlank() && message.isNotBlank(), onClick = { onCreate(subject.trim(), category, message.trim()) }) { Text(if (state.busy) "Creating…" else "Create ticket") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } })
}
