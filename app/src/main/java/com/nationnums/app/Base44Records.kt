package com.nationnums.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only mapping for records returned by the existing Base44 app.
 * The repository deliberately reads records only: wallet credits, purchases,
 * provider actions and other balance-changing operations must go through
 * authenticated server functions, never direct entity writes from the APK.
 */
internal data class WalletTransactionRecord(
    val id: String,
    val type: String,
    val amount: String,
    val balanceAfter: String,
    val status: String,
    val description: String,
    val createdAt: String
)

internal data class ServiceCatalogRecord(
    val id: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val service: String,
    val carrier: String,
    val priceTemporary: String,
    val priceRental: String,
    val listedAs: String,
    val available: Boolean,
    val status: String
)

internal data class VirtualNumberRecord(
    val id: String,
    val phoneNumber: String,
    val country: String,
    val service: String,
    val status: String,
    val expiresAt: String
)

internal data class NotificationRecord(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
)

internal data class SmsMessageRecord(
    val id: String,
    val code: String,
    val message: String,
    val sender: String,
    val createdAt: String
)

internal object Base44RecordParser {
    fun records(responseBody: String): List<JSONObject> {
        val trimmed = responseBody.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            when {
                trimmed.startsWith("[") -> jsonObjects(JSONArray(trimmed))
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    val array = sequenceOf("data", "items", "results", "records")
                        .mapNotNull { key -> root.optJSONArray(key) }
                        .firstOrNull()
                    when {
                        array != null -> jsonObjects(array)
                        root.has("id") -> listOf(root)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun walletTransaction(json: JSONObject) = WalletTransactionRecord(
        id = json.optString("id"),
        type = firstNonBlank(json, "type").ifBlank { "unknown" },
        amount = firstNonBlank(json, "amount").ifBlank { "0" },
        balanceAfter = firstNonBlank(json, "balance_after"),
        status = firstNonBlank(json, "status").ifBlank { "unknown" },
        description = json.optString("description"),
        createdAt = firstNonBlank(json, "created_date", "created_at")
    )

    fun serviceCatalog(json: JSONObject) = ServiceCatalogRecord(
        id = json.optString("id"),
        country = json.optString("country"),
        countryCode = firstNonBlank(json, "country_code"),
        flag = json.optString("flag"),
        service = json.optString("service"),
        carrier = json.optString("carrier"),
        priceTemporary = firstNonBlank(json, "price_temporary"),
        priceRental = firstNonBlank(json, "price_rental"),
        listedAs = firstNonBlank(json, "listed_as").ifBlank { "temporary" },
        available = json.optBoolean("available", true),
        status = firstNonBlank(json, "status").ifBlank { "active" }
    )

    fun virtualNumber(json: JSONObject) = VirtualNumberRecord(
        id = json.optString("id"),
        phoneNumber = json.optString("phone_number"),
        country = json.optString("country"),
        service = json.optString("service"),
        status = firstNonBlank(json, "status").ifBlank { "unknown" },
        expiresAt = json.optString("expires_at")
    )

    fun notification(json: JSONObject) = NotificationRecord(
        id = json.optString("id"),
        title = firstNonBlank(json, "title").ifBlank { "Nation Nums update" },
        body = firstNonBlank(json, "body"),
        type = firstNonBlank(json, "type").ifBlank { "system" },
        isRead = json.optBoolean("read", false),
        createdAt = firstNonBlank(json, "created_date", "created_at")
    )

    fun smsMessage(json: JSONObject) = SmsMessageRecord(
        id = json.optString("id"),
        code = firstNonBlank(json, "otp_code", "code"),
        message = firstNonBlank(json, "body", "message"),
        sender = json.optString("sender"),
        createdAt = firstNonBlank(json, "received_at", "created_date", "created_at")
    )

    private fun firstNonBlank(json: JSONObject, vararg keys: String): String =
        keys.firstNotNullOfOrNull { key ->
            json.optString(key).takeIf { it.isNotBlank() && it != "null" }
        }.orEmpty()

    private fun jsonObjects(array: JSONArray): List<JSONObject> =
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)
        }
}

/**
 * Safe read-only data access. Base44 row-level permissions remain authoritative.
 * Supply a real session token from the verified sign-in flow when it is wired in.
 */
internal class NationNumsRepository(accessToken: () -> String?) {
    private val api = Base44Api(accessToken)

    fun loadWalletTransactions(callback: (Base44Api.Result, List<WalletTransactionRecord>) -> Unit) {
        api.getEntityRecords("WalletTransaction") { result ->
            callback(result, if (result.success) Base44RecordParser.records(result.body).map(Base44RecordParser::walletTransaction) else emptyList())
        }
    }

    fun loadServiceCatalog(callback: (Base44Api.Result, List<ServiceCatalogRecord>) -> Unit) {
        api.getEntityRecords("ServiceCatalog", requireAuth = false) { result ->
            val records = if (result.success) Base44RecordParser.records(result.body)
                .map(Base44RecordParser::serviceCatalog)
                .filter { it.available && it.status.equals("active", ignoreCase = true) }
            else emptyList()
            callback(result, records)
        }
    }

    fun loadVirtualNumbers(callback: (Base44Api.Result, List<VirtualNumberRecord>) -> Unit) {
        api.getEntityRecords("VirtualNumber") { result ->
            callback(result, if (result.success) Base44RecordParser.records(result.body).map(Base44RecordParser::virtualNumber) else emptyList())
        }
    }

    fun loadNotifications(callback: (Base44Api.Result, List<NotificationRecord>) -> Unit) {
        api.getEntityRecords("Notification") { result ->
            callback(result, if (result.success) Base44RecordParser.records(result.body).map(Base44RecordParser::notification) else emptyList())
        }
    }

    fun loadSmsMessages(callback: (Base44Api.Result, List<SmsMessageRecord>) -> Unit) {
        api.getEntityRecords("SmsMessage") { result ->
            callback(result, if (result.success) Base44RecordParser.records(result.body).map(Base44RecordParser::smsMessage) else emptyList())
        }
    }
}
