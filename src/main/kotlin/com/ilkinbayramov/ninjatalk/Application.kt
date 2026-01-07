package com.ilkinbayramov.ninjatalk

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ilkinbayramov.ninjatalk.database.DatabaseFactory
import com.ilkinbayramov.ninjatalk.routes.authRoutes
import com.ilkinbayramov.ninjatalk.routes.blockRoutes
import com.ilkinbayramov.ninjatalk.routes.chatRoutes
import com.ilkinbayramov.ninjatalk.routes.userRoutes
import com.ilkinbayramov.ninjatalk.routes.webSocketRoutes
import com.ilkinbayramov.ninjatalk.services.AuthService
import com.ilkinbayramov.ninjatalk.services.ChatService
import com.ilkinbayramov.ninjatalk.services.FileService
import com.ilkinbayramov.ninjatalk.services.JwtService
import com.ilkinbayramov.ninjatalk.services.UserService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun main() {
    // Initialize database
    DatabaseFactory.init()

    // Initialize Firebase Admin SDK
    com.ilkinbayramov.ninjatalk.config.FirebaseConfig.initialize()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
}

fun Application.module() {

    val jwtSecret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
    val jwtService = JwtService(jwtSecret)
    val authService = AuthService(jwtService)
    val userService = UserService()
    val fileService = FileService()
    val notificationService = com.ilkinbayramov.ninjatalk.services.NotificationService(userService)
    val chatService =
            ChatService(com.ilkinbayramov.ninjatalk.services.BlockService(), notificationService)

    install(ContentNegotiation) { json() }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowHeader("Authorization")
    }

    install(io.ktor.server.websocket.WebSockets)

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT.require(Algorithm.HMAC256(jwtSecret)).build())
            validate { credential ->
                if (credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authRoutes(authService)
        userRoutes(userService, jwtService, fileService)
        chatRoutes(chatService)
        blockRoutes()
        webSocketRoutes(chatService)

        // Serve static files
        staticFiles("/uploads", File("uploads"))

        get("/") { call.respondText("NinjaTalk Backend is running!") }
    }
}
