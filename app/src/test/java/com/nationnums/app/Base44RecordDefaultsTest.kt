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

    @Test fun nullAndWhitespaceAmountsUseZeroFallback() {
        val nullAmount = Base44RecordParser.walletTransaction(JSONObject("""{"id":"t3","amount":null}"""))
        val whitespaceAmount = Base44RecordParser.walletTransaction(JSONObject("""{"id":"t4","amount":"   "}"""))
        assertEquals("0", nullAmount.amount)
        assertEquals("0", whitespaceAmount.amount)
    }

    @Test fun nullNotificationTextUsesSafeDefaults() {
        val record = Base44RecordParser.notification(JSONObject("""{"id":"n2","title":null,"body":null,"type":null}"""))
        assertEquals("Nation Nums update", record.title)
        assertEquals("", record.body)
        assertEquals("system", record.type)
    }
    @Test fun missingSmsFieldsUseSafeDefaults() {
        val record = Base44RecordParser.smsMessage(JSONObject("""{"id":"s1","otp_code":null,"code":"  ","body":null,"message":"  ","sender":null}"""))
        assertEquals("", record.code)
        assertEquals("", record.message)
        assertEquals("", record.sender)
        assertEquals("", record.createdAt)
    }

    @Test fun smsParserUsesFallbackKeysAndIgnoresLiteralNull() {
        val record = Base44RecordParser.smsMessage(JSONObject("""{"id":"s2","otp_code":"null","code":"482913","body":"","message":"Your verification code is 482913","received_at":null,"created_date":"2026-10-28"}"""))
        assertEquals("482913", record.code)
        assertEquals("Your verification code is 482913", record.message)
        assertEquals("2026-10-28", record.createdAt)
    }

    @Test fun recordParserIgnoresNonObjectArrayEntries() {
        assertEquals(1, Base44RecordParser.records("""[{"id":"v1"},null,3,"bad"]""").size)
        assertEquals(0, Base44RecordParser.records("""{"unexpected":"shape"}""").size)
    }

}
