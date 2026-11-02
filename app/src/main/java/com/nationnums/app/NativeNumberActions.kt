package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeNumberActions(backend: Base44Api, number: VirtualNumberRecord, onChanged: () -> Unit) {
    var busy by remember(number.id) { mutableStateOf(false) }
    var error by remember(number.id) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    if (number.status.equals("active", true) || number.status.equals("standby", true)) Column {
        OutlinedButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true; error = ""
                val result = backend.awaitFunction("fetchInbox", JSONObject().put("virtual_number_id", number.id))
                if (result.success) onChanged() else error = result.error ?: "Could not refresh SMS."
                busy = false
            }
        }) { Text(if (busy) "Checking SMS…" else "Check for SMS code") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    }
}
