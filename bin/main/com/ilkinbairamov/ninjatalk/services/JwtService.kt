package com.ilkinbairamov.ninjatalk.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.*

class JwtService(
        private val secret: String,
        private val issuer: String = "ninjatalk",
        private val audience: String = "ninjatalk-users",
        private val validityInMs: Long = 24 * 60 * 60 * 1000 // 24 hours
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: String): String {
        return JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withClaim("userId", userId)
                .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
                .sign(algorithm)
    }

    fun verifyToken(token: String): String? {
        return try {
            val verifier = JWT.require(algorithm).withAudience(audience).withIssuer(issuer).build()

            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("userId").asString()
        } catch (e: JWTVerificationException) {
            null
        }
    }
}
