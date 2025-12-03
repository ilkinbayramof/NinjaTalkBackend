package com.ilkinbairamov.ninjatalk.routes

import com.ilkinbairamov.ninjatalk.models.requests.LoginRequest
import com.ilkinbairamov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbairamov.ninjatalk.models.responses.ErrorResponse
import com.ilkinbairamov.ninjatalk.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()

                authService
                        .register(request)
                        .onSuccess { response -> call.respond(HttpStatusCode.Created, response) }
                        .onFailure { error ->
                            call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                            error = "REGISTRATION_FAILED",
                                            message = error.message ?: "Registration failed"
                                    )
                            )
                        }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                                error = "INVALID_REQUEST",
                                message =
                                        "Invalid request format: ${e.message} - ${e.cause?.message}"
                        )
                )
            }
        }

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()

                authService
                        .login(request)
                        .onSuccess { response -> call.respond(HttpStatusCode.OK, response) }
                        .onFailure { error ->
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    ErrorResponse(
                                            error = "LOGIN_FAILED",
                                            message = error.message ?: "Login failed"
                                    )
                            )
                        }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                                error = "INVALID_REQUEST",
                                message =
                                        "Invalid request format: ${e.message} - ${e.cause?.message}"
                        )
                )
            }
        }
    }
}
