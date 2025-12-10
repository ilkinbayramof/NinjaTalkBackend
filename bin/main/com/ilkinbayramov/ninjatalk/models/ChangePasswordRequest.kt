package com.ilkinbayramov.ninjatalk.models

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
