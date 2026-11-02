package com.nationnums.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeDeliveredOrder(backend: Base44Api, order: JSONObject, social: Boolean) {
    val scope = rememberCoroutineScope()
    val state = rememberNativeBackend(backend)
    var details by remember(order.toString()) { mutableStateOf(order) }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (social) details.text("listing_title") else details.text("proxy_type") + " proxy", style = MaterialTheme.typography.titleMedium)
        Text(details.text("status") + " · ₦" + details.text(if (social) "amount" else "cost"))
        SelectionContainer { Column {
            if (social) {
                val accounts = details.optJSONArray("accounts")
                if (accounts != null) for (i in 0 until accounts.length()) Text(accounts.optString(i))
            } else {
                Text(details.text("country_name"))
                Text("Host: " + details.text("proxy_host") + ":" + details.text("proxy_port"))
                Text("SOCKS port: " + details.text("proxy_port_socks"))
                Text("Login: " + details.text("proxy_login")); Text("Password: " + details.text("proxy_password"))
                Text(details.text("credentials_format")); Text("Remaining: " + details.text("gb_remaining") + " GB")
                if (details.text("expires_at").isNotBlank()) Text("Expires: " + details.text("expires_at"))
            }
        } }
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
        TextButton(enabled = !state.busy, onClick = { scope.launch {
            state.call(if (social) "sendSocialLogOrderWhatsApp" else "getProxyOrder", JSONObject().put(if (social) "order_id" else "id", details.text("id")))?.let { root -> if (!social) details = root.optJSONObject("order") ?: details }
        } }) { Text(if (state.busy) "Please wait…" else if (social) "Send to my verified WhatsApp" else "Refresh usage") }
    } }
}
