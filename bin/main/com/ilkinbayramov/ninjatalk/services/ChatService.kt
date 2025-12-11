package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.*
import com.ilkinbayramov.ninjatalk.models.Conversation
import com.ilkinbayramov.ninjatalk.models.Message
import java.util.UUID
import kotlin.random.Random
import org.jetbrains.exposed.sql.*

class ChatService(private val blockService: BlockService = BlockService()) {

        suspend fun createOrGetConversation(user1Id: String, user2Id: String): String =
                DatabaseFactory.dbQuery {
                        // Check if conversation already exists (either direction)
                        val existing =
                                Conversations.select {
                                                ((Conversations.user1Id eq user1Id) and
                                                        (Conversations.user2Id eq user2Id)) or
                                                        ((Conversations.user1Id eq user2Id) and
                                                                (Conversations.user2Id eq user1Id))
                                        }
                                        .singleOrNull()

                        if (existing != null) {
                                existing[Conversations.id]
                        } else {
                                // Create new conversation
                                val conversationId = UUID.randomUUID().toString()
                                Conversations.insert {
                                        it[id] = conversationId
                                        it[this.user1Id] = user1Id
                                        it[this.user2Id] = user2Id
                                        it[createdAt] = System.currentTimeMillis()
                                        it[lastMessageAt] = null
                                }

                                // Create anonymous identities for both users
                                createAnonymousIdentity(user1Id, conversationId)
                                createAnonymousIdentity(user2Id, conversationId)

                                conversationId
                        }
                }

        suspend fun getConversations(userId: String): List<Conversation> =
                DatabaseFactory.dbQuery {
                        // Get all conversations where user is participant
                        val conversations =
                                Conversations.select {
                                                (Conversations.user1Id eq userId) or
                                                        (Conversations.user2Id eq userId)
                                        }
                                        .orderBy(
                                                Conversations.lastMessageAt to
                                                        SortOrder.DESC_NULLS_LAST
                                        )

                        conversations.map { row ->
                                val conversationId = row[Conversations.id]
                                val otherUserId =
                                        if (row[Conversations.user1Id] == userId) {
                                                row[Conversations.user2Id]
                                        } else {
                                                row[Conversations.user1Id]
                                        }

                                // Get first message to determine who initiated
                                val firstMessage =
                                        Messages.select {
                                                        Messages.conversationId eq conversationId
                                                }
                                                .orderBy(Messages.timestamp to SortOrder.ASC)
                                                .limit(1)
                                                .singleOrNull()

                                // If current user sent first message, show real name
                                // Otherwise show anonymous name
                                val displayName =
                                        if (firstMessage?.get(Messages.senderId) == userId) {
                                                // User initiated - show real name from Users table
                                                val otherUser =
                                                        Users.select { Users.id eq otherUserId }
                                                                .singleOrNull()
                                                otherUser
                                                        ?.get(Users.email)
                                                        ?.substringBefore("@")
                                                        ?.replaceFirstChar { it.uppercase() }
                                                        ?: "Unknown"
                                        } else {
                                                // User received - show anonymous name
                                                getAnonymousName(otherUserId, conversationId)
                                        }

                                // Get last message
                                val lastMessage =
                                        Messages.select {
                                                        Messages.conversationId eq conversationId
                                                }
                                                .orderBy(Messages.timestamp to SortOrder.DESC)
                                                .limit(1)
                                                .singleOrNull()

                                // Count unread messages
                                val unreadCount =
                                        Messages.select {
                                                        (Messages.conversationId eq
                                                                conversationId) and
                                                                (Messages.senderId neq userId) and
                                                                (Messages.isRead eq false)
                                                }
                                                .count()
                                                .toInt()

                                Conversation(
                                        id = conversationId,
                                        otherUserId = otherUserId,
                                        otherUserAnonymousName = displayName,
                                        lastMessage = lastMessage?.get(Messages.content),
                                        lastMessageTimestamp = lastMessage?.get(Messages.timestamp),
                                        unreadCount = unreadCount
                                )
                        }
                }

        suspend fun getMessages(conversationId: String, userId: String): List<Message> =
                DatabaseFactory.dbQuery {
                        // Verify user is participant
                        val conversation =
                                Conversations.select { Conversations.id eq conversationId }
                                        .singleOrNull()
                                        ?: return@dbQuery emptyList()

                        val isParticipant =
                                conversation[Conversations.user1Id] == userId ||
                                        conversation[Conversations.user2Id] == userId

                        if (!isParticipant) return@dbQuery emptyList()

                        // Mark messages as read
                        Messages.update({
                                (Messages.conversationId eq conversationId) and
                                        (Messages.senderId neq userId)
                        }) { it[isRead] = true }

                        // Get messages
                        Messages.select { Messages.conversationId eq conversationId }
                                .orderBy(Messages.timestamp to SortOrder.ASC)
                                .map { row ->
                                        Message(
                                                id = row[Messages.id],
                                                conversationId = row[Messages.conversationId],
                                                senderId = row[Messages.senderId],
                                                content = row[Messages.content],
                                                timestamp = row[Messages.timestamp],
                                                isRead = row[Messages.isRead]
                                        )
                                }
                }

        suspend fun sendMessage(
                conversationId: String,
                senderId: String,
                content: String
        ): Message? =
                DatabaseFactory.dbQuery {
                        // Get receiver ID from conversation
                        val conversation =
                                Conversations.select { Conversations.id eq conversationId }
                                        .singleOrNull()
                                        ?: return@dbQuery null

                        val receiverId =
                                if (conversation[Conversations.user1Id] == senderId) {
                                        conversation[Conversations.user2Id]
                                } else {
                                        conversation[Conversations.user1Id]
                                }

                        // Check if sender is blocked by receiver (silently fail)
                        if (blockService.isBlocked(receiverId, senderId)) {
                                return@dbQuery null
                        }

                        val messageId = UUID.randomUUID().toString()
                        val timestamp = System.currentTimeMillis()

                        Messages.insert {
                                it[id] = messageId
                                it[this.conversationId] = conversationId
                                it[this.senderId] = senderId
                                it[this.content] = content
                                it[this.timestamp] = timestamp
                                it[isRead] = false
                        }

                        // Update conversation's lastMessageAt
                        Conversations.update({ Conversations.id eq conversationId }) {
                                it[lastMessageAt] = timestamp
                        }

                        Message(
                                id = messageId,
                                conversationId = conversationId,
                                senderId = senderId,
                                content = content,
                                timestamp = timestamp,
                                isRead = false
                        )
                }

        private fun getAnonymousName(userId: String, conversationId: String): String {
                val identity =
                        AnonymousIdentities.select {
                                        (AnonymousIdentities.userId eq userId) and
                                                (AnonymousIdentities.conversationId eq
                                                        conversationId)
                                }
                                .singleOrNull()

                return identity?.get(AnonymousIdentities.anonymousName) ?: "anon0000000"
        }

        private fun createAnonymousIdentity(userId: String, conversationId: String) {
                // Generate unique anonymous name
                var anonymousName: String
                var exists: Boolean

                do {
                        // Generate random 7-digit number
                        val randomNumber = Random.nextInt(1000000, 10000000)
                        anonymousName = "anon$randomNumber"

                        // Check if this name already exists in this conversation
                        exists =
                                AnonymousIdentities.select {
                                                (AnonymousIdentities.conversationId eq
                                                        conversationId) and
                                                        (AnonymousIdentities.anonymousName eq
                                                                anonymousName)
                                        }
                                        .count() > 0
                } while (exists)

                AnonymousIdentities.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[this.userId] = userId
                        it[this.conversationId] = conversationId
                        it[this.anonymousName] = anonymousName
                        it[createdAt] = System.currentTimeMillis()
                }
        }

        suspend fun deleteConversation(conversationId: String, userId: String): Boolean =
                DatabaseFactory.dbQuery {
                        // Verify user is participant
                        val conversation =
                                Conversations.select { Conversations.id eq conversationId }
                                        .singleOrNull()
                                        ?: return@dbQuery false

                        val isParticipant =
                                conversation[Conversations.user1Id] == userId ||
                                        conversation[Conversations.user2Id] == userId

                        if (!isParticipant) return@dbQuery false

                        // Delete all messages in conversation
                        val deleteMessagesSQL =
                                "DELETE FROM ${Messages.tableName} WHERE conversation_id = '$conversationId'"
                        org.jetbrains.exposed.sql.transactions.TransactionManager.current()
                                .exec(deleteMessagesSQL)

                        // Delete anonymous identities
                        val deleteIdentitiesSQL =
                                "DELETE FROM ${AnonymousIdentities.tableName} WHERE conversation_id = '$conversationId'"
                        org.jetbrains.exposed.sql.transactions.TransactionManager.current()
                                .exec(deleteIdentitiesSQL)

                        // Delete conversation
                        val deleteConversationSQL =
                                "DELETE FROM ${Conversations.tableName} WHERE id = '$conversationId'"
                        org.jetbrains.exposed.sql.transactions.TransactionManager.current()
                                .exec(deleteConversationSQL)

                        true
                }
}
