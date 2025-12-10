package com.ilkinbayramov.ninjatalk.models

import kotlinx.serialization.Serializable

@Serializable data class BlockUserRequest(val blockedUserId: String)
