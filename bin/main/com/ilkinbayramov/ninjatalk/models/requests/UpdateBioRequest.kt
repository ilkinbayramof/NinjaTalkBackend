package com.ilkinbayramov.ninjatalk.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBioRequest(
    val bio: String
)
