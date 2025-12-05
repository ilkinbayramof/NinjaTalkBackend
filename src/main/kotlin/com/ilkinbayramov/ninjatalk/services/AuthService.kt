package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.database.Users
import com.ilkinbayramov.ninjatalk.models.requests.LoginRequest
import com.ilkinbayramov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbayramov.ninjatalk.models.responses.AuthResponse
import com.ilkinbayramov.ninjatalk.utils.PasswordHasher
import java.util.UUID
import org.jetbrains.exposed.sql.*

class AuthService(private val jwtService: JwtService) {

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return dbQuery {
            val existingUser = Users.select { Users.email eq request.email }.singleOrNull()
            if (existingUser != null) {
                return@dbQuery Result.failure(Exception("Email already registered"))
            }

            val hashedPassword = PasswordHasher.hashPassword(request.password)
            val userId = UUID.randomUUID().toString()

            Users.insert {
                it[id] = userId
                it[email] = request.email
                it[password] = hashedPassword
                it[gender] = request.gender
                it[birthDate] = request.birthDate
                it[createdAt] = System.currentTimeMillis()
            }

            val token = jwtService.generateToken(userId, request.email)

            Result.success(AuthResponse(token = token, userId = userId, email = request.email))
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return dbQuery {
            val user =
                    Users.select { Users.email eq request.email }.singleOrNull()
                            ?: return@dbQuery Result.failure(Exception("Invalid email or password"))

            if (!PasswordHasher.checkPassword(request.password, user[Users.password])) {
                return@dbQuery Result.failure(Exception("Invalid email or password"))
            }

            val userId = user[Users.id]
            val token = jwtService.generateToken(userId, user[Users.email])

            Result.success(AuthResponse(token = token, userId = userId, email = user[Users.email]))
        }
    }
}
