package com.nationnums.app

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

internal class NativeBackendState(private val api: Base44Api) {
    var busy by mutableStateOf(false)
    var error by mutableStateOf("")
    var errorCode by mutableStateOf("")
    var errorData by mutableStateOf(JSONObject())
    var value by mutableStateOf(JSONObject())

    suspend fun call(name: String, payload: JSONObject = JSONObject()): JSONObject? {
        busy = true
        error = ""
        errorCode = ""
        errorData = JSONObject()
        try {
            val result = api.awaitFunction(name, payload)
            if (!result.success) {
                val body = runCatching { JSONObject(result.body) }.getOrNull() ?: JSONObject()
                error = result.error ?: body.optString("error").ifBlank { "Request failed. Please try again." }
                errorCode = body.optString("code")
                errorData = body
                return null
            }
            val root = runCatching { JSONObject(result.body) }.getOrNull()
            if (root == null) {
                error = "The server returned an invalid response. Please try again."
                return null
            }
            value = root
            return root
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            busy = false
        }
    }
}

@Composable
internal fun rememberNativeBackend(backend: Base44Api) = remember(backend) { NativeBackendState(backend) }

internal fun JSONObject.rows(key: String): List<JSONObject> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}

internal fun JSONObject.text(key: String): String = optString(key).takeUnless { it == "null" }.orEmpty()
