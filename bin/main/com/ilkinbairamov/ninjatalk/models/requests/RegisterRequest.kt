package com.ilkinbairamov.ninjatalk.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
        val email: String,
        val password: String,
        val gender: String, // "MALE" or "FEMALE"
        val birthDate: String // "2000-01-15" format
)
