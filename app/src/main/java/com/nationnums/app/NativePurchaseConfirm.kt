package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
internal fun NativePurchaseConfirm(price: String, service: String, busy: Boolean, error: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Confirm purchase") }, text = {
        Column { Text(service + " · ₦" + price); Text("This amount will be charged to your existing Nation Nums wallet. The server checks price and balance again before purchase."); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) }
    }, confirmButton = { Button(onClick = onConfirm, enabled = !busy) { Text(if (busy) "Purchasing…" else "Confirm ₦" + price) } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } })
}
