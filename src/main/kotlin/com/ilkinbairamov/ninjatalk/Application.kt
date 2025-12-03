package com.ilkinbairamov.ninjatalk

import com.ilkinbairamov.ninjatalk.routes.authRoutes
import com.ilkinbairamov.ninjatalk.services.AuthService
import com.ilkinbairamov.ninjatalk.services.JwtService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level

fun main() {
    embeddedServer(
                    Netty,
                    port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                    host = "0.0.0.0",
                    module = Application::module
            )
            .start(wait = true)
}

fun Application.module() {
    // Initialize PostgreSQL
    com.ilkinbayramov.ninjatalk.database.DatabaseFactory.init()

    // Initialize services
    val jwtSecret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
    val jwtService = JwtService(jwtSecret)
    val authService = AuthService(jwtService)

    // Install plugins
    install(ContentNegotiation) {
        json(
                kotlinx.serialization.json.Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
        )
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // For development - restrict in production
    }

    install(CallLogging) { level = Level.INFO }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                    text = "500: ${cause.message}",
                    status = HttpStatusCode.InternalServerError
            )
        }
    }

    // Configure routing
    routing {
        get("/") { call.respondText("NinjaTalk Backend API is running!", ContentType.Text.Plain) }

        get("/health") { call.respondText("OK", ContentType.Text.Plain) }

        // Auth routes
        authRoutes(authService)
    }
}
