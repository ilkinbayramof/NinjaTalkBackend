package com.ilkinbayramov.ninjatalk.services

import com.ilkinbairamov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbairamov.ninjatalk.models.requests.LoginRequest
import com.ilkinbairamov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbairamov.ninjatalk.models.responses.AuthResponse
import com.ilkinbairamov.ninjatalk.utils.PasswordHasher
import com.ilkinbayramov.ninjatalk.database.Users
import java.util.UUID
import org.jetbrains.exposed.sql.*

class AuthService(private val jwtService: JwtService) {

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return dbQuery {
            // Check if email already exists
            val existingUser = Users.select { Users.email eq request.email }.singleOrNull()
            if (existingUser != null) {
                return@dbQuery Result.failure(Exception("Email already registered"))
            }

            // Validate email format
            if (!isValidEmail(request.email)) {
                return@dbQuery Result.failure(Exception("Invalid email format"))
            }

            // Validate password length
            if (request.password.length < 6) {
                return@dbQuery Result.failure(Exception("Password must be at least 6 characters"))
            }

            // Validate gender
            if (request.gender !in listOf("MALE", "FEMALE")) {
                return@dbQuery Result.failure(Exception("Gender must be MALE or FEMALE"))
            }

            // Validate birth date format (basic check)
            if (!isValidDateFormat(request.birthDate)) {
                return@dbQuery Result.failure(
                        Exception("Invalid birth date format. Use YYYY-MM-DD")
                )
            }

            // Create new user
            val userId = UUID.randomUUID().toString()
            val passwordHash = PasswordHasher.hash(request.password)
            val createdAt = System.currentTimeMillis()

            Users.insert {
                it[id] = userId
                it[email] = request.email
                it[Users.passwordHash] = passwordHash
                it[gender] = request.gender
                it[birthDate] = request.birthDate
                it[Users.createdAt] = createdAt
            }

            // Generate token
            val token = jwtService.generateToken(userId)

            Result.success(AuthResponse(token = token, userId = userId, email = request.email))
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return dbQuery {
            // Find user by email
            val userRow =
                    Users.select { Users.email eq request.email }.singleOrNull()
                            ?: return@dbQuery Result.failure(Exception("Invalid email or password"))

            val userId = userRow[Users.id]
            val passwordHash = userRow[Users.passwordHash]

            // Verify password
            if (!PasswordHasher.verify(request.password, passwordHash)) {
                return@dbQuery Result.failure(Exception("Invalid email or password"))
            }

            // Generate token
            val token = jwtService.generateToken(userId)

            Result.success(AuthResponse(token = token, userId = userId, email = request.email))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun isValidDateFormat(date: String): Boolean {
        return date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
    }
}
