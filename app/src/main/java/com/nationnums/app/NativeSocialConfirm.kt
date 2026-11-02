package com.nationnums.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.json.JSONObject

@Composable
internal fun NativeSocialConfirm(listing: JSONObject, state: NativeBackendState, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var quantity by remember(listing) { mutableStateOf("1") }
    val units = quantity.toIntOrNull() ?: 0
    val allowed = units in 1..minOf(10, listing.optInt("stock", 0))
    AlertDialog(onDismissRequest = { if (!state.busy) onDismiss() }, title = { Text("Confirm account purchase") }, text = { Column {
        Text(listing.text("title"))
        OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, singleLine = true, enabled = !state.busy)
        Text("Total: ₦" + (listing.optDouble("unit_price", 0.0) * units))
        Text("The server verifies stock, price and your wallet before purchase.")
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
    } }, confirmButton = { Button(onClick = { onConfirm(units) }, enabled = allowed && !state.busy) { Text(if (state.busy) "Purchasing…" else "Confirm purchase") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } })
}
