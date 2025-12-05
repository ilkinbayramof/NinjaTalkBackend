package com.ilkinbayramov.ninjatalk.models.responses

import kotlinx.serialization.Serializable

@Serializable data class AuthResponse(val token: String, val userId: String, val email: String)

@Serializable data class ErrorResponse(val error: String, val message: String)
