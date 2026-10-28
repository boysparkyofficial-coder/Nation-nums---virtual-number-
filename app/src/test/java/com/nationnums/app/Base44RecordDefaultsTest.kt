package com.nationnums.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class Base44RecordDefaultsTest {
    @Test fun missingNotificationFieldsUseSafeDefaults() {
        val record = Base44RecordParser.notification(JSONObject("""{"id":"n1"}"""))
        assertEquals("Nation Nums update", record.title)
        assertEquals("system", record.type)
        assertEquals(false, record.isRead)
    }
}
