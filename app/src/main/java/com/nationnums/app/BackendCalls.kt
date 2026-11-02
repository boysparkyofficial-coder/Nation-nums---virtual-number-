package com.nationnums.app

import org.json.JSONObject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal suspend fun Base44Api.awaitFunction(name: String, payload: JSONObject = JSONObject()): Base44Api.Result =
    suspendCancellableCoroutine { continuation ->
        invokeFunction(name, payload) { result -> if (continuation.isActive) continuation.resume(result) }
    }
