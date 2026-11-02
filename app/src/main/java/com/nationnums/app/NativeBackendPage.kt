package com.nationnums.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun NativeBackendPage(title: String, state: NativeBackendState, modifier: Modifier = Modifier, onBack: () -> Unit, onRefresh: () -> Unit, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row { TextButton(onClick = onBack, enabled = !state.busy) { Text("Back") }; Spacer(Modifier.weight(1f)); TextButton(onClick = onRefresh, enabled = !state.busy) { Text("Refresh") } }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.error.isNotBlank()) item { Text(state.error, color = MaterialTheme.colorScheme.error) }
        content()
    }
}
