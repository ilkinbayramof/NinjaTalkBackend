package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.database.Users
import com.ilkinbayramov.ninjatalk.models.requests.LoginRequest
import com.ilkinbayramov.ninjatalk.models.requests.RegisterRequest
import com.ilkinbayramov.ninjatalk.models.responses.AuthResponse
import com.ilkinbayramov.ninjatalk.utils.PasswordHasher
import java.util.UUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.ilkinbayramov.ninjatalk.database.PasswordResetTokens

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

    suspend fun forgotPassword(email: String): Result<Unit> {
        return dbQuery {
            val user = Users.select { Users.email eq email }.singleOrNull()
                ?: return@dbQuery Result.success(Unit) // Başarılı dön ki e-posta tespiti yapılamasın

            val userId = user[Users.id]
            val token = UUID.randomUUID().toString()

            PasswordResetTokens.insert {
                it[this.id] = UUID.randomUUID().toString()
                it[this.userId] = userId
                it[this.token] = token
                it[createdAt] = System.currentTimeMillis()
            }

            // TODO: Sunucunuzun gerçek URL'si buraya yazılmalı.
            val resetLink = "http://localhost:8080/api/auth/reset-password-page?token=$token"
            EmailService.sendPasswordResetEmail(email, resetLink)

            Result.success(Unit)
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return dbQuery {
            val resetTokenRow = PasswordResetTokens.select { PasswordResetTokens.token eq token }.singleOrNull()
                ?: return@dbQuery Result.failure(Exception("Geçersiz veya süresi dolmuş bağlantı"))

            // 1 saat (3600000 ms) geçerlilik kontrolü
            val createdAt = resetTokenRow[PasswordResetTokens.createdAt]
            if (System.currentTimeMillis() - createdAt > 3600000) {
                PasswordResetTokens.deleteWhere { PasswordResetTokens.token eq token }
                return@dbQuery Result.failure(Exception("Bağlantı süresi dolmuş"))
            }

            val userId = resetTokenRow[PasswordResetTokens.userId]
            val hashedPassword = PasswordHasher.hashPassword(newPassword)

            Users.update({ Users.id eq userId }) {
                it[password] = hashedPassword
            }

            PasswordResetTokens.deleteWhere { PasswordResetTokens.token eq token }
            Result.success(Unit)
        }
    }
}
