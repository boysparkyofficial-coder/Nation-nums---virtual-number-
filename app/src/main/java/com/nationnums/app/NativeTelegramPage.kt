package com.nationnums.app

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeTelegramPage(backend: Base44Api, modifier: Modifier, onBack: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var linked by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    suspend fun status() { state.call("manageTelegramLink", JSONObject().put("action", "status"))?.let { linked = it.optBoolean("linked"); username = it.text("telegram_username") } }
    LaunchedEffect(Unit) { status() }
    NativeBackendPage("Telegram notifications", state, modifier, onBack, { scope.launch { status() } }) {
        item { Text(if (linked) "Linked to " + username else "Link Telegram to receive your order alerts.") }
        item { Button(enabled = !state.busy, onClick = { scope.launch {
            if (linked) state.call("manageTelegramLink", JSONObject().put("action", "unlink"))?.let { code = ""; link = ""; status() }
            else state.call("manageTelegramLink", JSONObject().put("action", "request_code"))?.let { code = it.text("link_code"); link = it.text("deep_link_url") }
        } }) { Text(if (linked) "Unlink Telegram" else "Get linking code") } }
        if (code.isNotBlank()) item {
            Text("Linking code: " + code + " · expires in 10 minutes")
            Text("Open the bot, complete linking, then tap Refresh.")
            TextButton(enabled = !state.busy && link.isNotBlank(), onClick = { val uri = Uri.parse(link); if (uri.scheme == "https" && uri.host == "t.me") runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { state.error = "Telegram could not be opened." } }) { Text("Open Telegram bot") }
        }
        if (linked) item { TextButton(enabled = !state.busy, onClick = { scope.launch { state.call("manageTelegramLink", JSONObject().put("action", "test_message"))?.let { notice = "Test alert sent to your linked Telegram." } } }) { Text("Send a test alert") } }
        if (notice.isNotBlank()) item { Text(notice) }
    }
}
