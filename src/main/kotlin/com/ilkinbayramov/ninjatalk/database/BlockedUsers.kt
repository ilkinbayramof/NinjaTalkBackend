package com.ilkinbayramov.ninjatalk.database

import org.jetbrains.exposed.sql.Table

object BlockedUsers : Table() {
    val id = varchar("id", 36)
    val blockerId = varchar("blocker_id", 36).references(Users.id)
    val blockedId = varchar("blocked_id", 36).references(Users.id)
    val blockedAt = long("blocked_at")

    override val primaryKey = PrimaryKey(id)
}
