package com.nationnums.app

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeServiceGate(backend: Base44Api, serviceKey: String, modifier: Modifier, onBack: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var loaded by remember(serviceKey) { mutableStateOf(false) }
    var allowed by remember(serviceKey) { mutableStateOf(false) }
    var message by remember(serviceKey) { mutableStateOf("") }
    suspend fun load() { state.call("mobileAccount", JSONObject().put("section", "services"))?.let { root -> val config = root.rows("items").find { it.text("service_key") == serviceKey }; allowed = config?.optBoolean("enabled", true) != false; message = config?.text("coming_soon_message").orEmpty(); loaded = true } }
    LaunchedEffect(serviceKey) { load() }
    if (loaded && allowed) content()
    else NativeBackendPage("Service availability", state, modifier, onBack, { scope.launch { load() } }) { if (loaded && !allowed) item { Text(message.ifBlank { "This service is currently unavailable. Existing orders remain accessible." }) } }
}
