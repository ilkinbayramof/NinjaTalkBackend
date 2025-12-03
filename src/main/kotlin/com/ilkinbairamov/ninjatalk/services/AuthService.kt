package com.ilkinbairamov.ninjatalk.services

import com.ilkinbairamov.ninjatalk.database.MongoDatabase
import com.ilkinbairamov.ninjatalk.models.User
import com.ilkinbairamov.ninjatalk.models.requests.LoginRequest
import com.ilkinbairamov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbairamov.ninjatalk.models.responses.AuthResponse
import com.ilkinbairamov.ninjatalk.utils.PasswordHasher
import org.litote.kmongo.eq

class AuthService(private val jwtService: JwtService) {

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        // Check if email already exists
        val existingUser = MongoDatabase.users.findOne(User::email eq request.email)
        if (existingUser != null) {
            return Result.failure(Exception("Email already registered"))
        }

        // Validate email format
        if (!isValidEmail(request.email)) {
            return Result.failure(Exception("Invalid email format"))
        }

        // Validate password length
        if (request.password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters"))
        }

        // Validate gender
        if (request.gender !in listOf("MALE", "FEMALE")) {
            return Result.failure(Exception("Gender must be MALE or FEMALE"))
        }

        // Validate birth date format (basic check)
        if (!isValidDateFormat(request.birthDate)) {
            return Result.failure(Exception("Invalid birth date format. Use YYYY-MM-DD"))
        }

        // Create new user
        val passwordHash = PasswordHasher.hash(request.password)
        val user =
                User(
                        email = request.email,
                        passwordHash = passwordHash,
                        gender = request.gender,
                        birthDate = request.birthDate
                )

        // Save to database
        MongoDatabase.users.insertOne(user)

        // Generate token
        val token = jwtService.generateToken(user.id)

        return Result.success(AuthResponse(token = token, userId = user.id, email = user.email))
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        // Find user by email
        val user =
                MongoDatabase.users.findOne(User::email eq request.email)
                        ?: return Result.failure(Exception("Invalid email or password"))

        // Verify password
        if (!PasswordHasher.verify(request.password, user.passwordHash)) {
            return Result.failure(Exception("Invalid email or password"))
        }

        // Generate token
        val token = jwtService.generateToken(user.id)

        return Result.success(AuthResponse(token = token, userId = user.id, email = user.email))
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun isValidDateFormat(date: String): Boolean {
        return date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
    }
}
