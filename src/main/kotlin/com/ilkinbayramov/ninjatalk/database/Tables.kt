package com.ilkinbayramov.ninjatalk.database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val gender = varchar("gender", 10)
    val birthDate = varchar("birth_date", 10)
    val bio = varchar("bio", 500).nullable()
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
