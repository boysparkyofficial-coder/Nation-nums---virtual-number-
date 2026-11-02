package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeBankTransfer(backend: Base44Api, onChanged: () -> Unit) {
    val state = rememberNativeBackend(backend)
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(JSONObject()) }
    LaunchedEffect(Unit) { state.call("mobileAccount", JSONObject().put("section", "profile"))?.optJSONObject("profile")?.let { profile -> account = JSONObject().put("account_number", profile.text("nationflow_account_number")).put("account_name", profile.text("nationflow_account_name")).put("bank_name", profile.text("nationflow_bank_name")) } }
    Column {
        Text("Bank transfer", style = MaterialTheme.typography.titleMedium)
        if (state.busy) LinearProgressIndicator()
        if (account.text("account_number").isNotBlank()) SelectionContainer { Column { Text(account.text("bank_name")); Text(account.text("account_name")); Text(account.text("account_number")); Text("Your wallet is credited only after the payment provider confirms your transfer.") } }
        else Button(enabled = !state.busy, onClick = { scope.launch {
            state.call("createDepositAccount")?.let { root -> account = root.optJSONObject("account") ?: JSONObject(); onChanged() }
        } }) { Text(if (state.busy) "Please wait…" else "Create my transfer account") }
        if (state.error.isNotBlank()) Text(state.error + " You can update and verify your details in Profile → Account details.", color = MaterialTheme.colorScheme.error)
    }
}
