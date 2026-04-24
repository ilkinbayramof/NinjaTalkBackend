package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.requests.ForgotPasswordRequest
import com.ilkinbayramov.ninjatalk.models.requests.LoginRequest
import com.ilkinbayramov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbayramov.ninjatalk.models.responses.ErrorResponse
import com.ilkinbayramov.ninjatalk.services.AuthService
import io.ktor.server.html.*
import kotlinx.html.*
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

        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            authService.forgotPassword(request.email)
            call.respond(HttpStatusCode.OK, mapOf("message" to "If the email is registered, a reset link will be sent."))
        }

        get("/reset-password-page") {
            val token = call.request.queryParameters["token"]
            if (token == null) {
                call.respondText("Token is missing", status = HttpStatusCode.BadRequest)
                return@get
            }
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title { +"Şifre Sıfırlama" }
                    style {
                        +"""
                            body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #121212; margin: 0; color: white; }
                            .container { background-color: #1E1E1E; padding: 30px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.5); width: 320px; }
                            h2 { text-align: center; color: #FFFFFF; font-weight: 600; margin-bottom: 24px; }
                            input[type=password] { width: 100%; padding: 12px; margin: 10px 0; border: none; border-radius: 8px; box-sizing: border-box; background-color: #2C2C2E; color: white; outline: none; }
                            input[type=submit] { width: 100%; background-color: #FF6E40; color: white; padding: 12px; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: bold; margin-top: 10px; }
                            input[type=submit]:hover { background-color: #E65C2E; }
                            a { color: #FF6E40; text-decoration: none; display: block; text-align: center; margin-top: 15px; }
                        """.trimIndent()
                    }
                }
                body {
                    div("container") {
                        h2 { +"Yeni Şifre" }
                        form(action = "/api/auth/reset-password", method = FormMethod.post) {
                            input(type = InputType.hidden, name = "token") { value = token }
                            input(type = InputType.password, name = "newPassword") {
                                placeholder = "Yeni Şifre"
                                required = true
                            }
                            input(type = InputType.password, name = "confirmPassword") {
                                placeholder = "Şifreyi Onayla"
                                required = true
                            }
                            input(type = InputType.submit) { value = "Şifreyi Sıfırla" }
                        }
                    }
                }
            }
        }

        post("/reset-password") {
            val parameters = call.receiveParameters()
            val token = parameters["token"] ?: return@post call.respondText("Eksik token", status = HttpStatusCode.BadRequest)
            val newPassword = parameters["newPassword"] ?: return@post call.respondText("Eksik şifre", status = HttpStatusCode.BadRequest)
            val confirmPassword = parameters["confirmPassword"] ?: return@post call.respondText("Eksik onay şifresi", status = HttpStatusCode.BadRequest)

            if (newPassword != confirmPassword) {
                call.respondHtml(HttpStatusCode.BadRequest) {
                    head { title { +"Hata" } }
                    body {
                        h2 { +"Hata: Şifreler eşleşmiyor!" }
                        a(href = "javascript:history.back()") { +"Geri Dön" }
                    }
                }
                return@post
            }

            authService.resetPassword(token, newPassword)
                .onSuccess {
                    call.respondHtml(HttpStatusCode.OK) {
                        head { title { +"Başarılı" } }
                        body {
                            h2 { +"Şifreniz başarıyla sıfırlandı!" }
                            p { +"Artık uygulamaya geri dönerek yeni şifrenizle giriş yapabilirsiniz." }
                        }
                    }
                }
                .onFailure {
                    call.respondHtml(HttpStatusCode.BadRequest) {
                        head { title { +"Hata" } }
                        body {
                            h2 { +"Hata: ${it.message}" }
                        }
                    }
                }
        }
    }
}
