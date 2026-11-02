package com.nationnums.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeNotificationRoutesTest {
    @Test fun numberLinksOpenOrders() { assertEquals("orders", NativeNotificationRoutes.destination("https://nation-nums.base44.app/numbers/123", "sms")) }
    @Test fun walletLinksOpenWallet() { assertEquals("wallet", NativeNotificationRoutes.destination("/transactions", "wallet")) }
    @Test fun externalLinksNeverOpenAnExternalPage() { assertEquals("notifications", NativeNotificationRoutes.destination("https://example.com/transactions", "system")) }
    @Test fun unknownLinksOpenNotifications() { assertEquals("notifications", NativeNotificationRoutes.destination(null, null)) }
}
