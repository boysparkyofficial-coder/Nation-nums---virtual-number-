package com.nationnums.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeAccountDetails(backend: Base44Api, auth: Base44AuthApi, modifier: Modifier, onBack: () -> Unit, onChanged: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    suspend fun load() { state.call("mobileAccount", JSONObject().put("section", "profile"))?.optJSONObject("profile")?.let { profile -> username = profile.text("username"); fullName = profile.text("full_name"); email = profile.text("email"); phone = profile.text("phone_number"); verified = profile.optBoolean("phone_verified") } }
    LaunchedEffect(Unit) { load() }
    NativeBackendPage("Account details", state, modifier, onBack, { scope.launch { load() } }) {
        item { Text(email); OutlinedTextField(fullName, { fullName = it.take(100) }, label = { Text("Full name") }, enabled = !state.busy); OutlinedTextField(username, { username = it.take(30) }, label = { Text("Username") }, enabled = !state.busy) }
        item { Button(enabled = !state.busy && username.isNotBlank(), onClick = { scope.launch { state.call("updateUsername", JSONObject().put("username", username.trim()).put("full_name", fullName.trim()))?.let { notice = "Account details saved."; onChanged(); load() } } }) { Text("Save account details") } }
        item { NativeWhatsAppVerification(backend, phone, verified) { scope.launch { load(); onChanged() } } }
        item { TextButton(enabled = !state.busy && email.isNotBlank(), onClick = {
            state.busy = true
            auth.requestPasswordReset(email) { state.busy = false; notice = "If this account exists, password-reset instructions have been sent to your email." }
        }) { Text("Send password-reset instructions") } }
        item { TextButton(enabled = !state.busy, onClick = { scope.launch {
            state.call("mobileSupport", JSONObject().put("action", "create").put("category", "account").put("subject", "Account deletion request").put("message", "I request deletion of my Nation Nums account and associated personal data. Please contact me through this support ticket for any required confirmation."))?.let { notice = "Your account deletion request was sent to support. You can follow it in Help & support." }
        } }) { Text("Request account deletion") } }
        if (notice.isNotBlank()) item { Text(notice) }
    }
}
