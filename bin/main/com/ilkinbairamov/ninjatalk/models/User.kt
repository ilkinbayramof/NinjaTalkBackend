package com.ilkinbayramov.ninjatalk.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class User(
        val id: String = UUID.randomUUID().toString(),
        val email: String,
        val passwordHash: String,
        val gender: String, // "MALE" or "FEMALE"
        val birthDate: String, // ISO 8601 format: "2000-01-15"
        val createdAt: Long = System.currentTimeMillis()
)
