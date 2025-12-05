package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.database.Users
import com.ilkinbayramov.ninjatalk.models.User
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
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
                    bio = it[Users.bio]
                )
            }
            .singleOrNull()
    }
    
    suspend fun updateBio(userId: String, bio: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[Users.bio] = bio
        } > 0
    }
    
    suspend fun getAllUsers(): List<User> = dbQuery {
        Users.selectAll()
            .map {
                User(
                    id = it[Users.id],
                    email = it[Users.email],
                    gender = it[Users.gender],
                    birthDate = it[Users.birthDate],
                    bio = it[Users.bio]
                )
            }
    }
}
