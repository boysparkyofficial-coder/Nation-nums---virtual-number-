package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeApiKeysPage(backend: Base44Api, modifier: Modifier, onBack: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var rawKey by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<JSONObject?>(null) }
    var action by remember { mutableStateOf("") }
    suspend fun load() { state.call("manageApiKeys", JSONObject().put("action", "list")) }
    suspend fun perform(payload: JSONObject) { state.call("manageApiKeys", payload)?.let { root -> rawKey = root.text("key"); name = ""; pending = null; load() } }
    LaunchedEffect(Unit) { load() }
    NativeBackendPage("My API keys", state, modifier, onBack, { scope.launch { load() } }) {
        item { OutlinedTextField(name, { name = it.take(60) }, label = { Text("New key name") }, enabled = !state.busy); Button(enabled = name.isNotBlank() && !state.busy, onClick = { scope.launch { perform(JSONObject().put("action", "create").put("name", name.trim())) } }) { Text("Create API key") } }
        val keys = state.value.rows("keys")
        if (!state.busy && keys.isEmpty() && state.error.isBlank()) item { Text("No API keys yet.") }
        items(keys, key = { it.text("id") }) { key -> Column {
            Text(key.text("name") + " · " + key.text("key_prefix") + " · " + key.text("status"))
            if (key.text("status") == "active") { TextButton(enabled = !state.busy, onClick = { action = "revoke"; pending = key }) { Text("Revoke") }; TextButton(enabled = !state.busy, onClick = { action = "regenerate"; pending = key }) { Text("Regenerate") } }
        } }
    }
    if (rawKey.isNotBlank()) AlertDialog(onDismissRequest = { rawKey = "" }, title = { Text("Copy your new API key") }, text = { Column { Text("This key is shown only once. Keep it private."); SelectionContainer { Text(rawKey) } } }, confirmButton = { TextButton(onClick = { rawKey = "" }) { Text("Done") } })
    pending?.let { key -> AlertDialog(onDismissRequest = { if (!state.busy) pending = null }, title = { Text("Confirm " + action) }, text = { Column { Text("The current key will stop working."); if (state.error.isNotBlank()) Text(state.error) } }, confirmButton = { Button(enabled = !state.busy, onClick = { scope.launch { perform(JSONObject().put("action", action).put("key_id", key.text("id"))) } }) { Text(if (state.busy) "Please wait…" else "Confirm") } }, dismissButton = { TextButton(enabled = !state.busy, onClick = { pending = null }) { Text("Cancel") } }) }
}
