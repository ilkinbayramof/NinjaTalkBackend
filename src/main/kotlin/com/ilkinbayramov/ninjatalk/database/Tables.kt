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

object Conversations : Table() {
    val id = varchar("id", 36)
    val user1Id = varchar("user1_id", 36).references(Users.id)
    val user2Id = varchar("user2_id", 36).references(Users.id)
    val createdAt = long("created_at")
    val lastMessageAt = long("last_message_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Messages : Table() {
    val id = varchar("id", 36)
    val conversationId = varchar("conversation_id", 36).references(Conversations.id)
    val senderId = varchar("sender_id", 36).references(Users.id)
    val content = text("content")
    val timestamp = long("timestamp")
    val isRead = bool("is_read").default(false)

    override val primaryKey = PrimaryKey(id)
}

object AnonymousIdentities : Table() {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(Users.id)
    val conversationId = varchar("conversation_id", 36).references(Conversations.id)
    val anonymousName = varchar("anonymous_name", 20) // anon1234567
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
