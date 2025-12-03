package com.ilkinbayramov.ninjatalk.database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 36).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val gender = varchar("gender", 10)
    val birthDate = varchar("birth_date", 10)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
