package com.nationnums.app

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

internal class NativeBackendState(private val api: Base44Api) {
    var busy by mutableStateOf(false)
    var error by mutableStateOf("")
    var value by mutableStateOf(JSONObject())
    suspend fun call(name: String, payload: JSONObject = JSONObject()): JSONObject? {
        busy = true; error = ""
        try {
            val result = api.awaitFunction(name, payload)
            if (!result.success) { error = result.error ?: "Request failed. Please try again."; return null }
            val root = runCatching { JSONObject(result.body) }.getOrNull()
            if (root == null) { error = "The server returned an invalid response. Please try again."; return null }
            value = root
            return root
        } catch (cancelled: CancellationException) { throw cancelled }
        finally { busy = false }
    }
}

@Composable
internal fun rememberNativeBackend(backend: Base44Api) = remember(backend) { NativeBackendState(backend) }

internal fun JSONObject.rows(key: String): List<JSONObject> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}

internal fun JSONObject.text(key: String): String = optString(key).takeUnless { it == "null" }.orEmpty()
