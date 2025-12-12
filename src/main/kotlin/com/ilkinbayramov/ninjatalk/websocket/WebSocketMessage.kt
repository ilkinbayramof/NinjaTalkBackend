package com.ilkinbayramov.ninjatalk.websocket

import com.ilkinbayramov.ninjatalk.models.Message
import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
        val type: String, // "send_message", "new_message", "typing", "message_read"
        val conversationId: String? = null,
        val content: String? = null,
        val message: Message? = null,
        val userId: String? = null
)
