package com.nationnums.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class Base44RecordDefaultsTest {
    @Test fun blankTransactionAndNumberStatusesUseUnknownFallback() {
        val transaction = Base44RecordParser.walletTransaction(JSONObject("""{"id":"t1","type":"","status":""}"""))
        val number = Base44RecordParser.virtualNumber(JSONObject("""{"id":"v1","status":""}"""))
        assertEquals("unknown", transaction.type)
        assertEquals("unknown", transaction.status)
        assertEquals("unknown", number.status)
        val blankAmount = Base44RecordParser.walletTransaction(JSONObject("""{"id":"t2","amount":""}"""))
        assertEquals("0", blankAmount.amount)
    }

    @Test fun missingNotificationFieldsUseSafeDefaults() {
        val record = Base44RecordParser.notification(JSONObject("""{"id":"n1"}"""))
        assertEquals("Nation Nums update", record.title)
        assertEquals("system", record.type)
        assertEquals(false, record.isRead)
    }
}
