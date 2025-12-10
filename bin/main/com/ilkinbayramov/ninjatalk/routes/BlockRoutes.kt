package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.BlockUserRequest
import com.ilkinbayramov.ninjatalk.services.BlockService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.blockRoutes(blockService: BlockService = BlockService()) {
    route("/api/users") {
        authenticate("auth-jwt") {
            // Block a user
            post("/block") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                        principal?.payload?.subject
                                ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<BlockUserRequest>()

                println(
                        "DEBUG BACKEND: Block request - blockerId: $userId, blockedId: ${request.blockedUserId}"
                )

                val success = blockService.blockUser(userId, request.blockedUserId)

                println("DEBUG BACKEND: Block operation result: $success")

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User blocked successfully"))
                } else {
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "User is already blocked")
                    )
                }
            }

            // Unblock a user
            delete("/unblock/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val blockerId =
                        principal?.payload?.subject
                                ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val blockedUserId =
                        call.parameters["userId"]
                                ?: return@delete call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing userId")
                                )

                println(
                        "DEBUG BACKEND: Unblock request - blockerId: $blockerId, blockedId: $blockedUserId"
                )

                val success = blockService.unblockUser(blockerId, blockedUserId)

                println("DEBUG BACKEND: Unblock operation result: $success")

                if (success) {
                    call.respond(
                            HttpStatusCode.OK,
                            mapOf("message" to "User unblocked successfully")
                    )
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User is not blocked"))
                }
            }

            // Get blocked users list
            get("/blocked") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                        principal?.payload?.subject
                                ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val blockedUsers = blockService.getBlockedUsers(userId)
                call.respond(HttpStatusCode.OK, blockedUsers)
            }
        }
    }
}
