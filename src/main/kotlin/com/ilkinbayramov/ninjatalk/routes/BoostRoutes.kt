package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.VerifyPurchaseRequest
import com.ilkinbayramov.ninjatalk.services.BoostService
import com.ilkinbayramov.ninjatalk.services.GrantBoostResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.boostRoutes(boostService: BoostService) {
    route("/api/boost") {
        authenticate("auth-jwt") {

            // Current boost state for the logged-in user
            get("/status") {
                val userId =
                        call.principal<JWTPrincipal>()?.payload?.subject
                                ?: return@get call.respond(HttpStatusCode.Unauthorized)

                call.respond(boostService.getStatus(userId))
            }

            // Redeem a store receipt. The client never decides how long the boost lasts.
            post("/verify") {
                val userId =
                        call.principal<JWTPrincipal>()?.payload?.subject
                                ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request =
                        try {
                            call.receive<VerifyPurchaseRequest>()
                        } catch (e: Exception) {
                            return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("error" to "Invalid request body")
                            )
                        }

                when (val result = boostService.grantBoost(userId, request)) {
                    is GrantBoostResult.Granted -> call.respond(HttpStatusCode.OK, result.status)
                    // Not an error: a retried call after a dropped response must stay safe
                    is GrantBoostResult.AlreadyRedeemed ->
                            call.respond(HttpStatusCode.OK, result.status)
                    is GrantBoostResult.UnknownProduct ->
                            call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("error" to "Unknown product: ${result.productId}")
                            )
                    is GrantBoostResult.VerificationFailed ->
                            call.respond(
                                    HttpStatusCode.PaymentRequired,
                                    mapOf("error" to "Purchase could not be verified")
                            )
                }
            }
        }
    }
}
