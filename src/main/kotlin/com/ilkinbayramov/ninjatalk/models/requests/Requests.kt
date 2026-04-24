package com.ilkinbayramov.ninjatalk.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
        val email: String,
        val password: String,
        val gender: String,
        val birthDate: String
)

@Serializable data class LoginRequest(val email: String, val password: String)

@Serializable data class ForgotPasswordRequest(val email: String)

@Serializable data class ResetPasswordRequest(val token: String, val newPassword: String)
