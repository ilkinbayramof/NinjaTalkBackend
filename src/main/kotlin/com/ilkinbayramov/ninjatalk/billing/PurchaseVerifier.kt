package com.ilkinbayramov.ninjatalk.billing

import com.ilkinbayramov.ninjatalk.models.VerifyPurchaseRequest

sealed class VerificationResult {
    /** The receipt is genuine and belongs to this purchase. */
    data object Valid : VerificationResult()

    data class Invalid(val reason: String) : VerificationResult()
}

/**
 * Checks that a purchase reported by the client actually happened.
 *
 * The client is never trusted: it only hands over a store transaction id, and the implementation
 * confirms it against the store (or RevenueCat) before any boost is granted.
 */
interface PurchaseVerifier {
    suspend fun verify(userId: String, request: VerifyPurchaseRequest): VerificationResult
}

/**
 * Accepts every receipt. Exists so the whole purchase flow can be exercised before the store
 * accounts are set up. [BillingConfig] refuses to select it once BILLING_MODE is not "dev".
 */
class DevPurchaseVerifier : PurchaseVerifier {
    override suspend fun verify(
            userId: String,
            request: VerifyPurchaseRequest
    ): VerificationResult {
        println(
                "⚠️ BILLING: DEV MODE - accepting unverified purchase " +
                        "user=$userId product=${request.productId} tx=${request.transactionId}"
        )
        return VerificationResult.Valid
    }
}

object BillingConfig {
    /** "dev" until the RevenueCat integration lands. */
    val mode: String = System.getenv("BILLING_MODE") ?: "dev"

    val isDevMode: Boolean = mode == "dev"

    fun createVerifier(): PurchaseVerifier =
            when (mode) {
                "dev" -> {
                    println("⚠️ BILLING: running in DEV mode - purchases are NOT verified")
                    DevPurchaseVerifier()
                }
                // TODO: "revenuecat" -> RevenueCatPurchaseVerifier(...)
                else ->
                        throw IllegalStateException(
                                "Unknown BILLING_MODE '$mode'. Supported values: dev"
                        )
            }
}
