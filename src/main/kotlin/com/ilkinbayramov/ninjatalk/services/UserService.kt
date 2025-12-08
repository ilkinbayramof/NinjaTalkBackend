package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.database.Users
import com.ilkinbayramov.ninjatalk.models.User
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class UserService {

    suspend fun getUserById(userId: String): User? = dbQuery {
        Users.select { Users.id eq userId }
                .mapNotNull {
                    User(
                            id = it[Users.id],
                            email = it[Users.email],
                            gender = it[Users.gender],
                            birthDate = it[Users.birthDate],
                            bio = it[Users.bio],
                            profileImageUrl = it[Users.profileImageUrl]
                    )
                }
                .singleOrNull()
    }

    suspend fun updateBio(userId: String, bio: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) { it[Users.bio] = bio } > 0
    }

    suspend fun updateProfileImage(userId: String, imageUrl: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) { it[Users.profileImageUrl] = imageUrl } > 0
    }

    suspend fun getAllUsers(): List<User> = dbQuery {
        Users.select { Users.isDeleted eq false }.map {
            User(
                    id = it[Users.id],
                    email = it[Users.email],
                    gender = it[Users.gender],
                    birthDate = it[Users.birthDate],
                    bio = it[Users.bio],
                    profileImageUrl = it[Users.profileImageUrl]
            )
        }
    }

    suspend fun softDeleteUser(userId: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[Users.isDeleted] = true
            it[Users.deletedAt] = System.currentTimeMillis()
        } > 0
    }

    suspend fun changePassword(
            userId: String,
            currentPassword: String,
            newPassword: String
    ): Boolean = dbQuery {
        // Get user's current password hash
        val user = Users.select { Users.id eq userId }.singleOrNull() ?: return@dbQuery false

        val storedPasswordHash = user[Users.password]

        // Verify current password using BCrypt
        if (!com.ilkinbayramov.ninjatalk.utils.PasswordHasher.checkPassword(
                        currentPassword,
                        storedPasswordHash
                )
        ) {
            return@dbQuery false
        }

        // Hash new password using BCrypt and update
        val newPasswordHash =
                com.ilkinbayramov.ninjatalk.utils.PasswordHasher.hashPassword(newPassword)
        Users.update({ Users.id eq userId }) { it[Users.password] = newPasswordHash } > 0
    }
}
