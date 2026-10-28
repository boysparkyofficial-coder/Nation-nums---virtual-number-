package com.nationnums.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Base44RecordParserTest {
    @Test
    fun parsesArrayResponse() {
        val result = Base44RecordParser.records(
            """[{"id":"tx-1","type":"deposit","amount":2500,"status":"success","description":"Wallet funding"}]"""
        )
        assertEquals(1, result.size)
        assertEquals("tx-1", Base44RecordParser.walletTransaction(result[0]).id)
        assertEquals("deposit", Base44RecordParser.walletTransaction(result[0]).type)
        assertEquals("2500", Base44RecordParser.walletTransaction(result[0]).amount)
    }

    @Test
    fun parsesWrappedResponse() {
        val result = Base44RecordParser.records(
            """{"data":[{"id":"n-1","title":"Order update","body":"Your order changed","read":true}]}"""
        )
        assertEquals(1, result.size)
        val notification = Base44RecordParser.notification(result[0])
        assertEquals("Order update", notification.title)
        assertEquals("Your order changed", notification.body)
        assertTrue(notification.isRead)
    }

    @Test
    fun mapsVirtualNumberFields() {
        val number = Base44RecordParser.virtualNumber(
            JSONObject("""{"id":"num-1","phone_number":"+12025550123","country":"United States","service":"WhatsApp","status":"active","expires_at":"2026-11-01"}""")
        )
        assertEquals("num-1", number.id)
        assertEquals("+12025550123", number.phoneNumber)
        assertEquals("United States", number.country)
        assertEquals("WhatsApp", number.service)
        assertEquals("active", number.status)
    }

    @Test
    fun mapsSmsFieldsUsingActualBase44SchemaNames() {
        val sms = Base44RecordParser.smsMessage(
            JSONObject("""{"id":"sms-1","otp_code":"483921","body":"Your code is 483921","sender":"WhatsApp","received_at":"2026-10-28T10:00:00Z"}""")
        )
        assertEquals("sms-1", sms.id)
        assertEquals("483921", sms.code)
        assertEquals("WhatsApp", sms.sender)
        assertTrue(sms.message.contains(sms.code))
        assertEquals("2026-10-28T10:00:00Z", sms.createdAt)
    }

    @Test
    fun supportsLegacySmsFieldAliases() {
        val sms = Base44RecordParser.smsMessage(
            JSONObject("""{"code":"111222","message":"Code 111222","created_at":"yesterday"}""")
        )
        assertEquals("111222", sms.code)
        assertEquals("Code 111222", sms.message)
        assertEquals("yesterday", sms.createdAt)
    }

    @Test
    fun handlesEmptyAndMalformedResponsesSafely() {
        assertTrue(Base44RecordParser.records("").isEmpty())
        assertTrue(Base44RecordParser.records("not-json").isEmpty())
        assertTrue(Base44RecordParser.records("""{"unexpected":"shape"}""").isEmpty())
    }

    @Test
    fun ignoresNonObjectItemsInArray() {
        val result = Base44RecordParser.records("""[null,4,{"id":"valid"}]""")
        assertEquals(1, result.size)
        assertFalse(result[0].optString("id").isBlank())
    }
}
