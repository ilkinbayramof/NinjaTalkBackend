package com.ilkinbayramov.ninjatalk.services

import com.ilkinbayramov.ninjatalk.database.BlockedUsers
import com.ilkinbayramov.ninjatalk.database.DatabaseFactory.dbQuery
import com.ilkinbayramov.ninjatalk.database.Users
import com.ilkinbayramov.ninjatalk.models.User
import java.util.UUID
import org.jetbrains.exposed.sql.*

class BlockService {

    suspend fun blockUser(blockerId: String, blockedId: String): Boolean = dbQuery {
        println("DEBUG BACKEND: blockUser called - blockerId: $blockerId, blockedId: $blockedId")

        // Check if already blocked
        val existingBlock =
                BlockedUsers.select {
                            (BlockedUsers.blockerId eq blockerId) and
                                    (BlockedUsers.blockedId eq blockedId)
                        }
                        .singleOrNull()

        if (existingBlock != null) {
            println("DEBUG BACKEND: User already blocked")
            return@dbQuery false // Already blocked
        }

        val blockId = UUID.randomUUID().toString()
        println("DEBUG BACKEND: Inserting new block record with id: $blockId")

        BlockedUsers.insert {
            it[id] = blockId
            it[BlockedUsers.blockerId] = blockerId
            it[BlockedUsers.blockedId] = blockedId
            it[blockedAt] = System.currentTimeMillis()
        }

        println("DEBUG BACKEND: Block record inserted successfully")
        true
    }

    suspend fun unblockUser(blockerId: String, blockedId: String): Boolean = dbQuery {
        println(
                "DEBUG BACKEND: unblockUser service called - blockerId: $blockerId, blockedId: $blockedId"
        )

        // Count records first using select (which works)
        val count =
                BlockedUsers.select {
                            (BlockedUsers.blockerId eq blockerId) and
                                    (BlockedUsers.blockedId eq blockedId)
                        }
                        .count()

        println("DEBUG BACKEND: Found $count records to delete")

        if (count > 0) {
            // Use Exposed's actual table name
            val tableName = BlockedUsers.tableName
            val sql =
                    "DELETE FROM $tableName WHERE blocker_id = '$blockerId' AND blocked_id = '$blockedId'"
            println("DEBUG BACKEND: Executing SQL: $sql")

            org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec(sql)

            println("DEBUG BACKEND: Delete executed successfully")
            true
        } else {
            println("DEBUG BACKEND: No records found to delete")
            false
        }
    }

    suspend fun getBlockedUsers(userId: String): List<User> = dbQuery {
        println("DEBUG BACKEND: getBlockedUsers called for userId: $userId")

        // Manual join to avoid ambiguity with multiple foreign keys
        val users =
                BlockedUsers.join(Users, JoinType.INNER, BlockedUsers.blockedId, Users.id)
                        .select { BlockedUsers.blockerId eq userId }
                        .map {
                            User(
                                    id = it[Users.id],
                                    email = it[Users.email],
                                    gender = it[Users.gender],
                                    birthDate = it[Users.birthDate],
                                    bio = it[Users.bio],
                                    profileImageUrl = it[Users.profileImageUrl]
                            )
                        }

        println("DEBUG BACKEND: Found ${users.size} blocked users")
        users
    }

    suspend fun isBlocked(blockerId: String, blockedId: String): Boolean = dbQuery {
        BlockedUsers.select {
                    (BlockedUsers.blockerId eq blockerId) and (BlockedUsers.blockedId eq blockedId)
                }
                .count() > 0
    }
}
