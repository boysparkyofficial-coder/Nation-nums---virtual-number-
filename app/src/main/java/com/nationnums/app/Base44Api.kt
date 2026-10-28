package com.nationnums.app

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Dependency-free client for the existing Nation Nums Base44 app.
 * It creates no second backend and stores no credentials. Callers must pass
 * a valid Base44 access token from the real Base44 authentication flow.
 * Never pass payment-provider or other server secrets from the app.
 */
internal object Base44Api {
    const val APP_ID = "6ac1b21923bd8a65822f7956"
    private const val BASE_URL = "https://nation-nums.base44.app"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    data class ApiResult(val statusCode: Int, val body: String, val successful: Boolean)

    /** Calls the documented Base44 entity collection endpoint on a background thread. */
    fun requestEntityCollection(
        entityName: String,
        method: String = "GET",
        accessToken: String? = null,
        query: Map<String, String> = emptyMap(),
        jsonBody: String? = null
    ): ApiResult {
        require(entityName.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) { "Invalid Base44 entity name" }
        val allowedMethods = setOf("GET", "POST", "PUT", "PATCH", "DELETE")
        val normalizedMethod = method.uppercase()
        require(normalizedMethod in allowedMethods) { "Unsupported HTTP method" }

        val queryString = query.entries.joinToString("&") { (key, value) ->
            encode(key) + "=" + encode(value)
        }
        val path = "/api/apps/" + APP_ID + "/entities/" + entityName
        val requestUrl = BASE_URL + path + if (queryString.isNotEmpty()) "?" + queryString else ""
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = normalizedMethod
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("X-App-Id", APP_ID)
            setRequestProperty("Accept", "application/json")
            if (!accessToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer " + accessToken)
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        return try {
            if (jsonBody != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            ApiResult(status, body, status in 200..299)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
