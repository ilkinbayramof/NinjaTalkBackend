package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.requests.LoginRequest
import com.ilkinbayramov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbayramov.ninjatalk.models.responses.ErrorResponse
import com.ilkinbayramov.ninjatalk.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()

            authService
                    .register(request)
                    .onSuccess { response -> call.respond(HttpStatusCode.Created, response) }
                    .onFailure { error ->
                        call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                        "REGISTRATION_FAILED",
                                        error.message ?: "Unknown error"
                                )
                        )
                    }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            authService
                    .login(request)
                    .onSuccess { response -> call.respond(HttpStatusCode.OK, response) }
                    .onFailure { error ->
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                ErrorResponse("LOGIN_FAILED", error.message ?: "Unknown error")
                        )
                    }
        }
    }
}
