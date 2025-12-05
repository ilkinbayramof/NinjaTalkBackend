package com.ilkinbayramov.ninjatalk.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val gender: String,
    val birthDate: String,
    val bio: String? = null
)
