package com.nationnums.app

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import org.json.JSONObject

internal class AndroidPushRegistration(context: Context, private val sessions: Base44SessionStore) {
    private val prefs = context.applicationContext.getSharedPreferences(NationNumsFirebaseMessagingService.PREFS, Context.MODE_PRIVATE)
    private val api = Base44Api { sessions.readToken() }
    private fun deviceId(): String = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }
    private fun appVersion(): String = runCatching { context.applicationContext.packageManager.getPackageInfo(context.applicationContext.packageName, 0).versionName ?: "unknown" }.getOrDefault("unknown")
    fun sync(callback: (String) -> Unit = {}) {
        if (sessions.readToken().isNullOrBlank()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> register(token, callback) }.addOnFailureListener { callback("Device notifications could not be registered. Reopen the app to retry.") }
    }
    fun register(token: String, callback: (String) -> Unit = {}) {
        prefs.edit().putString(NationNumsFirebaseMessagingService.KEY_FCM_TOKEN, token).apply()
        if (sessions.readToken().isNullOrBlank()) return
        api.invokeFunction("registerDeviceToken", JSONObject().put("action", "register").put("platform", "android").put("token", token).put("device_id", deviceId()).put("app_version", appVersion())) { result ->
            callback(if (!result.success) result.error ?: "Device registration failed." else if (JSONObject(result.body).optBoolean("delivery_configured")) "This device is linked for mobile notifications." else "This device is linked. Push sending is waiting for valid server credentials.")
        }
    }
    fun unlink(done: () -> Unit) {
        val finish = {
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                prefs.edit().remove(NationNumsFirebaseMessagingService.KEY_FCM_TOKEN).apply()
                done()
            }
        }
        if (sessions.readToken().isNullOrBlank()) finish()
        else api.invokeFunction("registerDeviceToken", JSONObject().put("action", "unregister").put("platform", "android").put("device_id", deviceId())) { finish() }
    }
}
