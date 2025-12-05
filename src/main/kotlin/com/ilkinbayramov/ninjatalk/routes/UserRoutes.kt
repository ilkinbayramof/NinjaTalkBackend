package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.requests.UpdateBioRequest
import com.ilkinbayramov.ninjatalk.services.JwtService
import com.ilkinbayramov.ninjatalk.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService, jwtService: JwtService) {
    
    route("/api/users") {
        
        // Get all users (for shuffle screen)
        get {
            val users = userService.getAllUsers()
            call.respond(users)
        }
        
        // Get current user profile
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject ?: return@get call.respond(
                    HttpStatusCode.Unauthorized
                )
                
                val user = userService.getUserById(userId)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }
            
            // Update bio
            put("/bio") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject ?: return@put call.respond(
                    HttpStatusCode.Unauthorized
                )
                
                val request = call.receive<UpdateBioRequest>()
                val success = userService.updateBio(userId, request.bio)
                
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Bio updated successfully"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update bio"))
                }
            }
        }
    }
}
