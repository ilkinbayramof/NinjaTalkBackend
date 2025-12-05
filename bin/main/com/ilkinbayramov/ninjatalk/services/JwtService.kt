package com.ilkinbayramov.ninjatalk.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtService(private val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: String, email: String): String {
        return JWT.create()
                .withSubject(userId)
                .withClaim("email", email)
                .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
                .sign(algorithm)
    }
}
