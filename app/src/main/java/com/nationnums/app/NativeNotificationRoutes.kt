package com.nationnums.app

import java.net.URI

/**
 * Converts verified push payload destinations into URLs on the existing Nation Nums website.
 * External hosts are never accepted as in-app destinations.
 */
internal object NativeNotificationRoutes {
    private const val SITE = "https://nation-nums.base44.app"

    fun destination(url: String?, type: String?): String {
        val parsed = runCatching { URI(url.orEmpty()) }.getOrNull()
        if (parsed != null &&
            parsed.scheme.equals("https", true) &&
            parsed.host.equals("nation-nums.base44.app", true) &&
            parsed.userInfo == null
        ) {
            return parsed.toString()
        }

        val path = when (type?.lowercase()) {
            "sms", "order", "number" -> "/orders"
            "wallet", "deposit", "payment" -> "/wallet"
            "security", "account" -> "/profile"
            else -> "/notifications"
        }
        return SITE + path
    }
}
