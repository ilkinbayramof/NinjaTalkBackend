package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.services.ChatService
import com.ilkinbayramov.ninjatalk.websocket.WebSocketConnectionManager
import com.ilkinbayramov.ninjatalk.websocket.WebSocketMessage
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.webSocketRoutes(chatService: ChatService) {
    authenticate("auth-jwt") {
        webSocket("/ws/chat") {
            val principal = call.principal<JWTPrincipal>()
            val userId =
                    principal?.payload?.subject
                            ?: run {
                                close(
                                        CloseReason(
                                                CloseReason.Codes.VIOLATED_POLICY,
                                                "Unauthorized"
                                        )
                                )
                                return@webSocket
                            }

            println("WS: User $userId attempting to connect")

            // Add connection
            WebSocketConnectionManager.addConnection(userId, this)

            try {
                // Send connection success
                send(Frame.Text(Json.encodeToString(WebSocketMessage(type = "connected"))))

                // Listen for incoming messages
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("WS: Received from $userId: $text")
                        handleIncomingMessage(text, userId, chatService)
                    }
                }
            } catch (e: Exception) {
                println("WS Error for user $userId: ${e.message}")
                e.printStackTrace()
            } finally {
                // Remove connection on disconnect
                WebSocketConnectionManager.removeConnection(userId)
            }
        }
    }
}

private suspend fun handleIncomingMessage(text: String, userId: String, chatService: ChatService) {
    try {
        val payload = Json.decodeFromString<WebSocketMessage>(text)
        println("WS: Processing message type: ${payload.type}")

        when (payload.type) {
            "send_message" -> {
                if (payload.conversationId == null || payload.content == null) {
                    println("WS: Missing conversationId or content")
                    return
                }

                // Save message to DB
                val message =
                        chatService.sendMessage(
                                conversationId = payload.conversationId,
                                senderId = userId,
                                content = payload.content
                        )

                if (message != null) {
                    println(
                            "WS: Message saved, broadcasting to conversation ${payload.conversationId}"
                    )

                    // Broadcast to conversation participants
                    val participants =
                            chatService.getConversationParticipants(payload.conversationId)
                    val response = WebSocketMessage(type = "new_message", message = message)

                    WebSocketConnectionManager.sendToUsers(
                            participants,
                            Json.encodeToString(response)
                    )

                    println("WS: Broadcasted to ${participants.size} participants")
                } else {
                    println("WS: Failed to save message (user might be blocked)")
                }
            }
            "typing" -> {
                if (payload.conversationId == null) return

                // Broadcast typing indicator to other participant
                val participants = chatService.getConversationParticipants(payload.conversationId)
                val otherUser = participants.firstOrNull { it != userId }

                if (otherUser != null) {
                    val response =
                            WebSocketMessage(
                                    type = "typing",
                                    conversationId = payload.conversationId,
                                    userId = userId
                            )
                    WebSocketConnectionManager.sendToUser(otherUser, Json.encodeToString(response))
                }
            }
            else -> {
                println("WS: Unknown message type: ${payload.type}")
            }
        }
    } catch (e: Exception) {
        println("WS: Message parse error: ${e.message}")
        e.printStackTrace()
    }
}
