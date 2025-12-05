package com.ilkinbayramov.ninjatalk

import com.ilkinbayramov.ninjatalk.database.DatabaseFactory
import com.ilkinbayramov.ninjatalk.routes.authRoutes
import com.ilkinbayramov.ninjatalk.services.AuthService
import com.ilkinbayramov.ninjatalk.services.JwtService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()

    val jwtSecret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
    val jwtService = JwtService(jwtSecret)
    val authService = AuthService(jwtService)

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
    }

    routing {
        authRoutes(authService)
        
        get("/") {
            call.respondText("NinjaTalk Backend is running!")
        }
    }
}
