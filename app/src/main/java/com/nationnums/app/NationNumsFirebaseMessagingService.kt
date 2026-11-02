package com.nationnums.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Nation Nums push messages from the existing Firebase project.
 * The FCM registration token is retained locally until the authenticated
 * Nation Nums device-registration endpoint is connected.
 */
class NationNumsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
        // TODO: sync token to the authenticated Nation Nums backend endpoint.
        // Do not send it to a guessed route or expose backend credentials in the APK.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Nation Nums"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: return
        showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nation Nums alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wallet, order, number and account updates"
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        launchIntent.action = MainActivity.ACTION_OPEN_NOTIFICATIONS
        launchIntent.putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
        data["type"]?.let { launchIntent.putExtra("notification_type", it) }
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    companion object {
        const val PREFS = "nation_nums_push"
        const val KEY_FCM_TOKEN = "fcm_registration_token"
        private const val CHANNEL_ID = "nation_nums_updates_v2"
        private const val NOTIFICATION_ID = 7402
    }
}
