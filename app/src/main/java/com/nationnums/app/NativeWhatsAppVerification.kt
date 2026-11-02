package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeWhatsAppVerification(backend: Base44Api, savedPhone: String, verified: Boolean, onChanged: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var phone by remember(savedPhone) { mutableStateOf(savedPhone) }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    Column {
        Text(if (verified && phone == savedPhone) "WhatsApp number verified" else "Verify your WhatsApp number")
        OutlinedTextField(phone, { phone = it; sent = false; notice = "" }, label = { Text("Nigerian phone number") }, enabled = !state.busy, singleLine = true)
        TextButton(enabled = !state.busy && phone.isNotBlank(), onClick = { scope.launch {
            state.call("verifyPhone", JSONObject().put("action", "send").put("phone_number", phone.trim()))?.let { sent = true; notice = "A verification code was sent to your WhatsApp." }
        } }) { Text(if (state.busy) "Please wait…" else "Send verification code") }
        if (sent) {
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(8) }, label = { Text("WhatsApp code") }, enabled = !state.busy, singleLine = true)
            Button(enabled = !state.busy && code.isNotBlank(), onClick = { scope.launch { state.call("verifyPhone", JSONObject().put("action", "verify").put("phone_number", phone.trim()).put("code", code))?.let { notice = "WhatsApp number verified."; sent = false; code = ""; onChanged() } } }) { Text("Verify number") }
        }
        if (notice.isNotBlank()) Text(notice)
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
    }
}
