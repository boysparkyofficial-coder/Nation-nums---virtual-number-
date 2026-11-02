package com.nationnums.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeNotificationRoutesTest {
    @Test fun numberLinksOpenOrders() { assertEquals("https://nation-nums.base44.app/numbers/123", NativeNotificationRoutes.destination("https://nation-nums.base44.app/numbers/123", "sms")) }
    @Test fun walletLinksOpenWallet() { assertEquals("https://nation-nums.base44.app/wallet", NativeNotificationRoutes.destination("/transactions", "wallet")) }
    @Test fun externalLinksNeverOpenAnExternalPage() { assertEquals("https://nation-nums.base44.app/notifications", NativeNotificationRoutes.destination("https://example.com/transactions", "system")) }
    @Test fun unknownLinksOpenNotifications() { assertEquals("https://nation-nums.base44.app/notifications", NativeNotificationRoutes.destination(null, null)) }
}
