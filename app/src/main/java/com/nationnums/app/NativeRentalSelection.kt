package com.nationnums.app

import androidx.compose.runtime.*
import org.json.JSONObject

internal class NativeRentalSelection {
    var server by mutableStateOf<JSONObject?>(null)
    var duration by mutableStateOf<JSONObject?>(null)
    var country by mutableStateOf<JSONObject?>(null)
    var service by mutableStateOf<JSONObject?>(null)
    val stage get() = when { server == null -> 0; duration == null -> 1; country == null -> 2; service == null -> 3; else -> 4 }
    fun payload() = JSONObject().put("plan_type", "rental").put("service_key", "rent_number").apply {
        server?.let { put("server_id", it.text("id")) }
        duration?.let { put("duration_hours", it.optInt("duration_hours")) }
        country?.let { put("country", it.text("country")); put("country_code", it.text("country_code")); put("flag", it.text("flag")) }
        service?.let { put("service", it.text("service")) }
    }
    fun back(): Boolean { when (stage) { 4 -> service = null; 3 -> country = null; 2 -> duration = null; 1 -> server = null; else -> return false }; return true }
    fun choose(entry: JSONObject) { when (stage) { 0 -> server = entry; 1 -> duration = entry; 2 -> country = entry; 3 -> service = entry } }
}
