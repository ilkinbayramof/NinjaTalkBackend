package com.ilkinbairamov.ninjatalk.models.responses

import kotlinx.serialization.Serializable

@Serializable data class ErrorResponse(val error: String, val message: String)
