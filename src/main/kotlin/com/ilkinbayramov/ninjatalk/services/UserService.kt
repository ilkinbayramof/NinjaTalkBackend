package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.database.Users
import com.ilkinbayramov.ninjatalk.models.User
import com.ilkinbayramov.ninjatalk.utils.PasswordHasher
import java.util.UUID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class UserService(
        private val boostService: BoostService? = null,
        private val fileService: FileService = FileService()
) {

    /**
     * Users with a running boost count as premium for now. Splitting boost from a real premium
     * subscription later only means changing this one place.
     */
    private suspend fun boostedUserIds(): Set<String> =
            boostService?.activeBoostUserIds() ?: emptySet()

    /** Accounts created before usernames existed fall back to the e-mail prefix. */
    private fun displayNameOf(row: org.jetbrains.exposed.sql.ResultRow): String =
            row[Users.displayName]
                    ?: row[Users.email].substringBefore("@").replaceFirstChar { it.uppercase() }

    suspend fun getUserById(userId: String): User? {
        val boosted = boostedUserIds()
        return dbQuery {
            Users.select { (Users.id eq userId) and (Users.isDeleted eq false) }
                .mapNotNull {
                    User(
                            id = it[Users.id],
                            email = it[Users.email],
                            displayName = displayNameOf(it),
                            gender = it[Users.gender],
                            birthDate = it[Users.birthDate],
                            bio = it[Users.bio],
                            profileImageUrl = it[Users.profileImageUrl],
                            isPremium = it[Users.isPremium] || it[Users.id] in boosted,
                            fcmToken = it[Users.fcmToken]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun updateBio(userId: String, bio: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) { it[Users.bio] = bio } > 0
    }

    /** Null when the name was taken, otherwise whether the row was updated. */
    suspend fun updateDisplayName(userId: String, name: String): Boolean? = dbQuery {
        if (isUsernameTaken(name, excludeUserId = userId)) return@dbQuery null

        Users.update({ (Users.id eq userId) and (Users.isDeleted eq false) }) {
            it[Users.displayName] = name
        } > 0
    }

    suspend fun updateProfileImage(userId: String, imageUrl: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) { it[Users.profileImageUrl] = imageUrl } > 0
    }

    suspend fun getAllUsers(): List<User> {
        val boosted = boostedUserIds()
        return dbQuery {
            Users.select { Users.isDeleted eq false }.map {
                User(
                        id = it[Users.id],
                        email = it[Users.email],
                        displayName = displayNameOf(it),
                        gender = it[Users.gender],
                        birthDate = it[Users.birthDate],
                        bio = it[Users.bio],
                        profileImageUrl = it[Users.profileImageUrl],
                        isPremium = it[Users.isPremium] || it[Users.id] in boosted,
                        fcmToken = it[Users.fcmToken]
                )
            }
        }
    }

    suspend fun getFilteredUsers(
            minAge: Int? = null,
            maxAge: Int? = null,
            gender: String? = null
    ): List<User> {
        val boosted = boostedUserIds()
        return dbQuery {
        val currentYear = java.time.LocalDate.now().year

        Users.select { Users.isDeleted eq false }
                .map {
                    User(
                            id = it[Users.id],
                            email = it[Users.email],
                            displayName = displayNameOf(it),
                            gender = it[Users.gender],
                            birthDate = it[Users.birthDate],
                            bio = it[Users.bio],
                            profileImageUrl = it[Users.profileImageUrl],
                            isPremium = it[Users.isPremium] || it[Users.id] in boosted,
                            fcmToken = it[Users.fcmToken]
                    )
                }
                .filter { user ->
                    // Gender filter
                    val genderMatch =
                            gender == null || user.gender.equals(gender, ignoreCase = true)

                    // Age filter - birthDate format: "1990-05-15"
                    val ageMatch =
                            if (minAge != null || maxAge != null) {
                                try {
                                    val birthYear = user.birthDate.split("-")[0].toInt()
                                    val userAge = currentYear - birthYear

                                    val minMatch = minAge == null || userAge >= minAge
                                    val maxMatch = maxAge == null || userAge <= maxAge
                                    minMatch && maxMatch
                                } catch (e: Exception) {
                                    false // Invalid date format
                                }
                            } else {
                                true
                            }

                    genderMatch && ageMatch
                }
        }
    }

    /**
     * Deletes the account by stripping everything personal from it.
     *
     * The row itself has to stay, because messages reference it and dropping it would take the
     * other participant's chat history with it. What is left behind is an id, the message text the
     * two people exchanged, and an anon#### label - nothing that identifies the person. The e-mail
     * is released so they can sign up again, and the new account is unrelated to this one.
     */
    suspend fun softDeleteUser(userId: String): Boolean {
        // Read the image path before the row is wiped so the file can be removed from disk too
        val imageUrl = dbQuery {
            Users.select { Users.id eq userId }.singleOrNull()?.get(Users.profileImageUrl)
        }

        val updated = dbQuery {
            Users.update({ Users.id eq userId }) {
                it[Users.email] = "deleted_$userId@deleted.local"
                // Random password: the account can never be signed into again, even by accident
                it[Users.password] = PasswordHasher.hashPassword(UUID.randomUUID().toString())
                it[Users.profileImageUrl] = null
                it[Users.bio] = null
                it[Users.gender] = ""
                it[Users.birthDate] = ""
                it[Users.fcmToken] = null
                it[Users.isPremium] = false
                it[Users.isDeleted] = true
                it[Users.deletedAt] = System.currentTimeMillis()
            } > 0
        }

        if (updated && imageUrl != null) {
            fileService.deleteProfileImage(imageUrl)
        }

        return updated
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

    suspend fun updateFcmToken(userId: String, token: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) { it[Users.fcmToken] = token } > 0
    }
}
