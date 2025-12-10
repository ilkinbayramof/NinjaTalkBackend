package com.ilkinbayramov.ninjatalk.models

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
        val id: String,
        val otherUserId: String,
        val otherUserAnonymousName: String,
        val lastMessage: String?,
        val lastMessageTimestamp: Long?,
        val unreadCount: Int
)

@Serializable
data class Message(
        val id: String,
        val conversationId: String,
        val senderId: String,
        val content: String,
        val timestamp: Long,
        val isRead: Boolean
)

@Serializable data class SendMessageRequest(val conversationId: String, val content: String)

@Serializable data class CreateConversationRequest(val otherUserId: String)
