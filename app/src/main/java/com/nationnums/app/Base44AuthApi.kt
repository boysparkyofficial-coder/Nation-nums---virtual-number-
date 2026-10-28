package com.nationnums.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.Executors

/**
 * Stores the Base44 access token encrypted with a key held by Android Keystore.
 * This is separate from the app-only unlock passcode.
 */
internal class Base44SessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nation_nums_session", Context.MODE_PRIVATE)
    @Volatile private var transientToken: String? = null

    @Synchronized
    fun saveToken(token: String, rememberMe: Boolean = true) {
        val cleanToken = token.trim()
        require(cleanToken.isNotBlank() && !cleanToken.equals("null", ignoreCase = true)) {
            "Cannot store an empty or invalid session token."
        }
        if (!rememberMe) {
            clearPersistedToken()
            transientToken = cleanToken
            return
        }
        // Persist successfully before publishing the in-memory session. If encryption
        // fails, the caller must not observe a partially established login session.
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(cleanToken.toByteArray(StandardCharsets.UTF_8))
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
        transientToken = cleanToken
    }

    @Synchronized
    fun readToken(): String? {
        transientToken?.let { return it }
        val ivEncoded = prefs.getString(KEY_IV, null)
        val tokenEncoded = prefs.getString(KEY_TOKEN, null)
        if (ivEncoded == null || tokenEncoded == null) {
            // Remove orphaned ciphertext/IV left by an interrupted or partial write.
            clearPersistedToken()
            return null
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(ivEncoded, Base64.NO_WRAP))
            )
            String(
                cipher.doFinal(Base64.decode(tokenEncoded, Base64.NO_WRAP)),
                StandardCharsets.UTF_8
            ).trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
                ?: run {
                    clear()
                    null
                }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    @Synchronized
    fun clear() {
        transientToken = null
        clearPersistedToken()
    }

    @Synchronized
    private fun clearPersistedToken() {
        prefs.edit().remove(KEY_IV).remove(KEY_TOKEN).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "nation_nums_base44_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_IV = "token_iv"
        const val KEY_TOKEN = "token_ciphertext"
    }
}

/** Email/password login against the route used by the official Base44 JS SDK. */
internal class Base44AuthApi(
    private val sessionStore: Base44SessionStore
) {
    data class User(
        val id: String,
        val email: String,
        val username: String,
        val role: String
    )

    data class Result(
        val success: Boolean,
        val statusCode: Int,
        val user: User? = null,
        val error: String? = null
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun login(email: String, password: String, rememberMe: Boolean = true, callback: (Result) -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            callback(Result(false, 0, error = "Enter your email and password."))
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = URL("$BASE_URL/auth/login").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.doOutput = true
                connection.setRequestProperty("X-App-Id", APP_ID)
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = JSONObject().put("email", cleanEmail).put("password", password).toString()
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.let { input ->
                    BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
                }.orEmpty()
                if (status in 200..299) {
                    val json = JSONObject(raw)
                    val token = json.optString("access_token").takeUnless { it.isBlank() || it == "null" }.orEmpty()
                    val userJson = json.optJSONObject("user")
                    if (token.isBlank() || userJson == null || userJson.optString("id").isBlank() || userJson.optString("id") == "null") {
                        Result(false, status, error = "The sign-in response was incomplete. Please try again.")
                    } else {
                        sessionStore.saveToken(token, rememberMe)
                        Result(
                            true,
                            status,
                            User(
                                id = userJson.optString("id"),
                                email = userJson.optString("email", cleanEmail),
                                username = userJson.optString("username", userJson.optString("full_name", "")),
                                role = userJson.optString("role", "user")
                            )
                        )
                    }
                } else {
                    Result(false, status, error = when (status) {
                        400, 401 -> "Email or password is incorrect. Check your details and try again."
                        403 -> "Your account may need email verification before signing in."
                        429 -> "Too many sign-in attempts. Wait a little and try again."
                        in 500..599 -> "Sign-in is temporarily unavailable. Please try again shortly."
                        else -> "Could not sign in (HTTP $status). Please try again."
                    })
                }
            } catch (_: Exception) {
                Result(false, 0, error = "Could not connect securely to Nation Nums. Check your internet and try again.")
            } finally {
                connection?.disconnect()
            }
            mainHandler.post { callback(result) }
        }
    }

    /** Validates a stored session and retrieves the current Base44 user. */
    fun currentUser(callback: (Result) -> Unit) {
        val token = sessionStore.readToken()
        if (token.isNullOrBlank()) {
            mainHandler.post { callback(Result(false, 401, error = "Sign in to your Nation Nums account.")) }
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = URL("$BASE_URL/entities/User/me").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 12000
                connection.readTimeout = 20000
                connection.setRequestProperty("X-App-Id", APP_ID)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.let { input ->
                    BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
                }.orEmpty()
                if (status in 200..299) {
                    val root = JSONObject(raw)
                    val userJson = root.optJSONObject("user") ?: root.optJSONObject("data") ?: root
                    val userId = userJson.optString("id").trim()
                        .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }.orEmpty()
                    val userEmail = userJson.optString("email").trim()
                        .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }.orEmpty()
                    if (userId.isBlank() && userEmail.isBlank()) {
                        sessionStore.clear()
                        Result(false, status, error = "Could not validate your account session. Please sign in again.")
                    } else {
                        Result(true, status, User(
                            id = userId,
                            email = userEmail,
                            username = userJson.optString("username").trim()
                                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
                                ?: userJson.optString("full_name").trim()
                                    .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }.orEmpty(),
                            role = userJson.optString("role").trim()
                                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) } ?: "user"
                        ))
                    }
                } else {
                    if (status == 401 || status == 403) sessionStore.clear()
                    Result(false, status, error = when (status) {
                        401, 403 -> "Your session has expired. Please sign in again."
                        429 -> "Too many requests. Wait a little and try again."
                        in 500..599 -> "Nation Nums is temporarily unavailable. Please try again."
                        else -> "Could not validate your session (HTTP $status)."
                    })
                }
            } catch (_: Exception) {
                Result(false, 0, error = "Could not validate your session. Check your internet and try again.")
            } finally {
                connection?.disconnect()
            }
            mainHandler.post { callback(result) }
        }
    }

    fun logout() {
        sessionStore.clear()
    }

    private companion object {
        const val APP_ID = "6ac1b21923bd8a65822f7956"
        const val BASE_URL = "https://nation-nums.base44.app/api/apps/$APP_ID"
    }
}
