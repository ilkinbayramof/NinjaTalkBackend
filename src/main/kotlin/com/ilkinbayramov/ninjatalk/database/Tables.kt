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
    val isDeleted = bool("is_deleted").default(false)
    val deletedAt = long("deleted_at").nullable()
    val isPremium = bool("is_premium").default(false)
    val fcmToken = varchar("fcm_token", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object Conversations : Table() {
    val id = varchar("id", 36)
    val user1Id = varchar("user1_id", 36).references(Users.id)
    val user2Id = varchar("user2_id", 36).references(Users.id)
    val createdAt = long("created_at")
    val lastMessageAt = long("last_message_at").nullable()

    // Per-user soft delete: conversation is hidden for that user until a newer
    // message arrives. Rows stay intact for the other participant.
    val user1DeletedAt = long("user1_deleted_at").nullable()
    val user2DeletedAt = long("user2_deleted_at").nullable()

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

/**
 * A purchased boost window. Rows are never deleted so refunds and support requests stay auditable;
 * a refunded boost is marked instead.
 */
object Boosts : Table() {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(Users.id)
    val productId = varchar("product_id", 50) // boost_5 / boost_15 / boost_30
    val platform = varchar("platform", 10) // android / ios
    // Store transaction id. Unique so the same receipt can never be redeemed twice.
    val transactionId = varchar("transaction_id", 255).uniqueIndex()
    val startedAt = long("started_at")
    val expiresAt = long("expires_at")
    val status = varchar("status", 20).default("active") // active / refunded
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PasswordResetTokens : Table() {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(Users.id)
    val token = varchar("token", 255)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
