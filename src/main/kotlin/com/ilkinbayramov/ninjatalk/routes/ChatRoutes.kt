package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.CreateConversationRequest
import com.ilkinbayramov.ninjatalk.models.SendMessageRequest
import com.ilkinbayramov.ninjatalk.services.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(chatService: ChatService) {
        route("/api/chat") {
                authenticate("auth-jwt") {

                        // Get all conversations for current user
                        get("/conversations") {
                                val principal = call.principal<JWTPrincipal>()
                                val userId =
                                        principal?.payload?.subject
                                                ?: return@get call.respond(
                                                        HttpStatusCode.Unauthorized
                                                )

                                val conversations = chatService.getConversations(userId)
                                call.respond(conversations)
                        }

                        // Create or get conversation with another user
                        post("/conversations") {
                                val principal = call.principal<JWTPrincipal>()
                                val userId =
                                        principal?.payload?.subject
                                                ?: return@post call.respond(
                                                        HttpStatusCode.Unauthorized
                                                )

                                val request = call.receive<CreateConversationRequest>()
                                val conversationId =
                                        chatService.createOrGetConversation(
                                                userId,
                                                request.otherUserId
                                        )

                                call.respond(
                                        HttpStatusCode.OK,
                                        mapOf("conversationId" to conversationId)
                                )
                        }

                        // Get messages for a conversation
                        get("/conversations/{id}/messages") {
                                val principal = call.principal<JWTPrincipal>()
                                val userId =
                                        principal?.payload?.subject
                                                ?: return@get call.respond(
                                                        HttpStatusCode.Unauthorized
                                                )

                                val conversationId =
                                        call.parameters["id"]
                                                ?: return@get call.respond(
                                                        HttpStatusCode.BadRequest,
                                                        mapOf("error" to "Missing conversation ID")
                                                )

                                val messages = chatService.getMessages(conversationId, userId)
                                call.respond(messages)
                        }

                        // Send a message
                        post("/messages") {
                                val principal = call.principal<JWTPrincipal>()
                                val userId =
                                        principal?.payload?.subject
                                                ?: return@post call.respond(
                                                        HttpStatusCode.Unauthorized
                                                )

                                val request = call.receive<SendMessageRequest>()
                                val message =
                                        chatService.sendMessage(
                                                request.conversationId,
                                                userId,
                                                request.content
                                        )

                                if (message != null) {
                                        call.respond(HttpStatusCode.Created, message)
                                } else {
                                        // Silently succeed (user is blocked)
                                        call.respond(HttpStatusCode.OK, mapOf("status" to "sent"))
                                }
                        }

                        // Delete conversation
                        delete("/conversations/{id}") {
                                val principal = call.principal<JWTPrincipal>()
                                val userId =
                                        principal?.payload?.subject
                                                ?: return@delete call.respond(
                                                        HttpStatusCode.Unauthorized
                                                )

                                val conversationId =
                                        call.parameters["id"]
                                                ?: return@delete call.respond(
                                                        HttpStatusCode.BadRequest,
                                                        mapOf("error" to "Missing conversation ID")
                                                )

                                val success = chatService.deleteConversation(conversationId, userId)

                                if (success) {
                                        call.respond(
                                                HttpStatusCode.OK,
                                                mapOf("message" to "Conversation deleted")
                                        )
                                } else {
                                        call.respond(
                                                HttpStatusCode.Forbidden,
                                                mapOf("error" to "Not authorized")
                                        )
                                }
                        }
                }
        }
}
