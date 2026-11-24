package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select

/**
 * Case-insensitive, and only among live accounts: a deleted account keeps its name for the chats it
 * appears in, but does not hold the name hostage for everyone else.
 *
 * Call this inside a transaction. It is a fast pre-check that gives a clean error message - the
 * unique index on the table is what actually guarantees uniqueness under concurrent sign-ups.
 */
fun isUsernameTaken(username: String, excludeUserId: String?): Boolean {
    val target = username.trim().lowercase()

    return Users.select { (Users.isDeleted eq false) and (Users.displayName.lowerCase() eq target) }
            .any { row -> row[Users.id] != excludeUserId }
}
