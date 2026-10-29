package com.nationnums.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class Base44RecordFallbackTest {
    @Test fun missingNotificationFieldsUseSafeDefaults() {
        val value = Base44RecordParser.notification(JSONObject("{\"id\":\"notice-1\"}"))
        assertEquals("Nation Nums update", value.title)
        assertEquals("system", value.type)
        assertEquals(false, value.isRead)
    }

    @Test fun missingVirtualNumberStatusIsUnknown() {
        val value = Base44RecordParser.virtualNumber(JSONObject("{\"id\":\"num-1\"}"))
        assertEquals("unknown", value.status)
    }
}
