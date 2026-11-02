package com.nationnums.app

import java.net.URI

internal object NativeNotificationRoutes {
    fun destination(url: String?, type: String?): String {
        val parsed = runCatching { URI(url.orEmpty()) }.getOrNull()
        val allowed = parsed != null && (parsed.host == null || (parsed.scheme == "https" && parsed.host.equals("nation-nums.base44.app", true)))
        val path = if (allowed) parsed?.path.orEmpty() else ""
        return when {
            path.startsWith("/numbers/") || path == "/my-numbers" || path == "/inbox" -> "orders"
            path == "/transactions" || path == "/deposit/complete" -> "wallet"
            path == "/notifications" -> "notifications"
            type == "sms" || type == "order" -> "orders"
            type == "wallet" -> "wallet"
            else -> "notifications"
        }
    }
}
