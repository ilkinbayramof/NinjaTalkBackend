package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.billing.PurchaseVerifier
import com.ilkinbayramov.ninjatalk.billing.VerificationResult
import com.ilkinbayramov.ninjatalk.database.Boosts
import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.models.BoostProduct
import com.ilkinbayramov.ninjatalk.models.BoostStatusResponse
import com.ilkinbayramov.ninjatalk.models.VerifyPurchaseRequest
import java.util.UUID
import org.jetbrains.exposed.sql.*

sealed class GrantBoostResult {
    data class Granted(val status: BoostStatusResponse) : GrantBoostResult()

    /** The receipt was already redeemed; the caller still gets the current status. */
    data class AlreadyRedeemed(val status: BoostStatusResponse) : GrantBoostResult()

    data class UnknownProduct(val productId: String) : GrantBoostResult()

    data class VerificationFailed(val reason: String) : GrantBoostResult()
}

class BoostService(private val verifier: PurchaseVerifier) {

    /**
     * Grants a boost after the receipt checks out. Buying while a boost is still running extends it
     * rather than throwing the remaining time away.
     */
    suspend fun grantBoost(userId: String, request: VerifyPurchaseRequest): GrantBoostResult {
        val product =
                BoostProduct.fromProductId(request.productId)
                        ?: return GrantBoostResult.UnknownProduct(request.productId)

        // A receipt that was already redeemed must not grant a second boost
        val existing = dbQuery {
            Boosts.select { Boosts.transactionId eq request.transactionId }.singleOrNull() != null
        }
        if (existing) {
            println("⚠️ BOOST: transaction ${request.transactionId} already redeemed")
            return GrantBoostResult.AlreadyRedeemed(getStatus(userId))
        }

        when (val result = verifier.verify(userId, request)) {
            is VerificationResult.Invalid -> {
                println("❌ BOOST: verification failed for $userId - ${result.reason}")
                return GrantBoostResult.VerificationFailed(result.reason)
            }
            VerificationResult.Valid -> Unit
        }

        val now = System.currentTimeMillis()
        // Stack on top of any time the user still has left
        val startsAt = maxOf(now, activeBoostRow(userId)?.get(Boosts.expiresAt) ?: now)
        val expiresAt = startsAt + product.minutes * 60_000L

        dbQuery {
            Boosts.insert {
                it[id] = UUID.randomUUID().toString()
                it[this.userId] = userId
                it[productId] = product.productId
                it[platform] = request.platform
                it[transactionId] = request.transactionId
                it[this.startedAt] = now
                it[this.expiresAt] = expiresAt
                it[status] = "active"
                it[createdAt] = now
            }
        }

        println("🚀 BOOST: granted ${product.minutes}min to $userId, expires at $expiresAt")
        return GrantBoostResult.Granted(getStatus(userId))
    }

    suspend fun getStatus(userId: String): BoostStatusResponse {
        val row = activeBoostRow(userId) ?: return BoostStatusResponse(isActive = false)
        val expiresAt = row[Boosts.expiresAt]
        val remainingMillis = expiresAt - System.currentTimeMillis()

        return BoostStatusResponse(
                isActive = true,
                productId = row[Boosts.productId],
                expiresAt = expiresAt,
                remainingSeconds = (remainingMillis / 1000).coerceAtLeast(0)
        )
    }

    suspend fun hasActiveBoost(userId: String): Boolean = activeBoostRow(userId) != null

    /** Ids of every user currently boosted, used to sort them to the top of Shuffle. */
    suspend fun activeBoostUserIds(): Set<String> = dbQuery {
        val now = System.currentTimeMillis()
        Boosts.select { (Boosts.status eq "active") and (Boosts.expiresAt greater now) }
                .map { it[Boosts.userId] }
                .toSet()
    }

    /** The furthest-out active boost for this user, or null when none is running. */
    private suspend fun activeBoostRow(userId: String): ResultRow? = dbQuery {
        val now = System.currentTimeMillis()
        Boosts.select {
                    (Boosts.userId eq userId) and
                            (Boosts.status eq "active") and
                            (Boosts.expiresAt greater now)
                }
                .orderBy(Boosts.expiresAt to SortOrder.DESC)
                .limit(1)
                .singleOrNull()
    }
}
