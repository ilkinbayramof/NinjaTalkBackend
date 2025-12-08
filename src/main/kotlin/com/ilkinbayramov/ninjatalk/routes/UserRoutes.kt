package com.ilkinbayramov.ninjatalk.routes

import com.ilkinbayramov.ninjatalk.models.requests.UpdateBioRequest
import com.ilkinbayramov.ninjatalk.services.FileService
import com.ilkinbayramov.ninjatalk.services.JwtService
import com.ilkinbayramov.ninjatalk.services.UserService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService, jwtService: JwtService, fileService: FileService) {

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
                val userId =
                        principal?.payload?.subject
                                ?: return@get call.respond(HttpStatusCode.Unauthorized)

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
                val userId =
                        principal?.payload?.subject
                                ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<UpdateBioRequest>()
                val success = userService.updateBio(userId, request.bio)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Bio updated successfully"))
                } else {
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to update bio")
                    )
                }
            }

            // Upload profile image
            post("/profile-image") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                        principal?.payload?.subject
                                ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val multipart = call.receiveMultipart()
                var imageUrl: String? = null

                multipart.forEachPart { part ->
                    println("Received part: name=${part.name}, contentType=${part.contentType}")
                    if (part is PartData.FileItem) {
                        println(
                                "FileItem: name=${part.name}, originalFileName=${part.originalFileName}"
                        )
                        if (part.name == "image") {
                            imageUrl = fileService.saveProfileImage(part, userId)
                            println("Saved image: $imageUrl")
                        }
                    }
                    part.dispose()
                }

                if (imageUrl != null) {
                    val success = userService.updateProfileImage(userId, imageUrl!!)
                    if (success) {
                        call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                        "message" to "Profile image uploaded successfully",
                                        "imageUrl" to imageUrl
                                )
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to update profile image")
                        )
                    }
                } else {
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid image format or missing image")
                    )
                }
            }

            // Delete profile image
            delete("/profile-image") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                        principal?.payload?.subject
                                ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val user = userService.getUserById(userId)
                if (user?.profileImageUrl != null) {
                    fileService.deleteProfileImage(user.profileImageUrl)
                    userService.updateProfileImage(userId, "")
                }

                call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Profile image deleted successfully")
                )
            }

            // Change password
            put("/password") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                        principal?.payload?.subject
                                ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request =
                        call.receive<com.ilkinbayramov.ninjatalk.models.ChangePasswordRequest>()

                // Validate new password
                if (request.newPassword.length < 6) {
                    return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "New password must be at least 6 characters")
                    )
                }

                val success =
                        userService.changePassword(
                                userId,
                                request.currentPassword,
                                request.newPassword
                        )

                if (success) {
                    call.respond(
                            HttpStatusCode.OK,
                            mapOf("message" to "Password changed successfully")
                    )
                } else {
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Current password is incorrect")
                    )
                }
            }
        }
    }
}
