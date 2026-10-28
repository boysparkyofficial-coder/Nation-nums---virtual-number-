package com.nationnums.app

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Small native HTTP client for the existing Nation Nums Base44 app.
 *
 * This client intentionally contains no provider credentials and never creates a
 * second data store. Callers must supply the signed-in user's Base44 access token;
 * do not pass an app secret or a provider secret here.
 *
 * Network requests run off the main thread. API contracts for authentication and
 * backend functions must be verified before adding those flows to the UI.
 */
internal class Base44Api(
    private val accessToken: () -> String?
) {
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    data class Result(
        val success: Boolean,
        val statusCode: Int,
        val body: String,
        val error: String? = null
    )

    fun getEntityRecords(
        entityName: String,
        query: Map<String, String> = emptyMap(),
        callback: (Result) -> Unit
    ) {
        requireSafeEntityName(entityName)
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val path = "/entities/${encode(entityName)}" +
            if (queryString.isNotBlank()) "?$queryString" else ""
        request("GET", path, null, callback)
    }

    fun createEntityRecord(
        entityName: String,
        record: JSONObject,
        callback: (Result) -> Unit
    ) {
        requireSafeEntityName(entityName)
        request("POST", "/entities/${encode(entityName)}", record.toString(), callback)
    }

    /**
     * Exposes a deliberately narrow request surface. Only the app's entity
     * collection paths are accepted here, preventing accidental calls to
     * unrelated hosts or arbitrary URL paths.
     */
    private fun request(
        method: String,
        path: String,
        jsonBody: String?,
        callback: (Result) -> Unit
    ) {
        val token = accessToken()?.trim().orEmpty()
        if (token.isBlank()) {
            mainHandler.post {
                callback(Result(false, 0, "", "Sign in to your Nation Nums account first."))
            }
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("X-App-Id", APP_ID)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")
                if (jsonBody != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.let { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
                }.orEmpty()
                Result(status in 200..299, status, body, if (status in 200..299) null else safeError(status, body))
            } catch (e: Exception) {
                Result(false, 0, "", "Could not reach Nation Nums. Check your internet connection and try again.")
            } finally {
                connection?.disconnect()
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun safeError(status: Int, body: String): String {
        // Avoid displaying raw server payloads that may contain implementation details.
        return when (status) {
            401, 403 -> "Your session may have expired. Sign in again."
            404 -> "This Nation Nums resource was not found."
            429 -> "Too many requests. Please wait and try again."
            in 500..599 -> "Nation Nums is temporarily unavailable. Try again shortly."
            else -> "Request failed (HTTP $status). Please try again."
        }
    }

    private fun requireSafeEntityName(name: String) {
        require(name.matches(Regex("[A-Za-z][A-Za-z0-9_]{0,63}"))) {
            "Invalid Base44 entity name."
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val BASE_URL = "https://nation-nums.base44.app/api/apps/6ac1b21923bd8a65822f7956"
        const val APP_ID = "6ac1b21923bd8a65822f7956"
        const val CONNECT_TIMEOUT_MS = 12_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
