package com.ilkinbairamov.ninjatalk.models.responses

import kotlinx.serialization.Serializable

@Serializable data class AuthResponse(val token: String, val userId: String, val email: String)
