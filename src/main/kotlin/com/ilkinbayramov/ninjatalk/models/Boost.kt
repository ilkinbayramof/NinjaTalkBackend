package com.ilkinbayramov.ninjatalk.models

import kotlinx.serialization.Serializable

/**
 * The boost catalog lives on the server so a tampered client cannot claim a 30 minute boost while
 * paying for a 5 minute one. Product ids must match the ones configured in the stores.
 */
enum class BoostProduct(val productId: String, val minutes: Int) {
    BOOST_5("boost_5", 5),
    BOOST_15("boost_15", 15),
    BOOST_30("boost_30", 30);

    companion object {
        fun fromProductId(productId: String): BoostProduct? =
                entries.find { it.productId == productId }
    }
}

@Serializable
data class VerifyPurchaseRequest(
        val productId: String,
        /** Store transaction id. In dev mode any unique value is accepted. */
        val transactionId: String,
        val platform: String = "android"
)

@Serializable
data class BoostStatusResponse(
        val isActive: Boolean,
        val productId: String? = null,
        /** Epoch millis when the boost ends, null when no boost is active. */
        val expiresAt: Long? = null,
        /** Seconds left, so the client does not depend on its own clock being correct. */
        val remainingSeconds: Long = 0
)
