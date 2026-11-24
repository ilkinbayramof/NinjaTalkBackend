package com.ilkinbayramov.ninjatalk.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val databaseUrl =
                System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ninjatalk"
        val user = System.getenv("PGUSER") ?: "postgres"
        val password = System.getenv("PGPASSWORD") ?: "postgres"

        val database = Database.connect(createHikariDataSource(databaseUrl, user, password))

        transaction(database) {
            println("DATABASE: Creating tables...")

            // Force create tables (will skip if already exist with IF NOT EXISTS)
            SchemaUtils.create(Users, Conversations, Messages, AnonymousIdentities, BlockedUsers, PasswordResetTokens, Boosts)

            // Migration: per-user conversation delete columns (existing installs)
            exec("ALTER TABLE conversations ADD COLUMN IF NOT EXISTS user1_deleted_at BIGINT")
            exec("ALTER TABLE conversations ADD COLUMN IF NOT EXISTS user2_deleted_at BIGINT")

            exec(
                    "ALTER TABLE messages ADD COLUMN IF NOT EXISTS hidden_for_recipient BOOLEAN NOT NULL DEFAULT FALSE"
            )

            exec("ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(20)")
            // Backfill accounts created before usernames existed. LEFT(..., 20) matters: an
            // e-mail prefix can be longer than the column. Already-deleted accounts are skipped:
            // their e-mail is an internal placeholder and would surface as the chat name.
            exec(
                    """
                UPDATE users
                SET display_name = LEFT(
                        UPPER(LEFT(SPLIT_PART(email, '@', 1), 1))
                        || SUBSTRING(SPLIT_PART(email, '@', 1) FROM 2),
                        20)
                WHERE display_name IS NULL AND is_deleted = FALSE
            """.trimIndent()
            )

            // The backfill can produce collisions (ilkin@a.com and ilkin@b.com both become
            // "Ilkin"), so the losers get an id fragment appended before uniqueness is enforced.
            exec(
                    """
                WITH ranked AS (
                    SELECT id,
                           ROW_NUMBER() OVER (
                               PARTITION BY LOWER(display_name) ORDER BY created_at
                           ) AS rn
                    FROM users
                    WHERE is_deleted = FALSE AND display_name IS NOT NULL
                )
                UPDATE users u
                SET display_name = LEFT(u.display_name, 13) || '_' || LEFT(u.id, 6)
                FROM ranked r
                WHERE u.id = r.id AND r.rn > 1
            """.trimIndent()
            )

            // Case-insensitive and only among live accounts, so a deleted account does not keep
            // its name out of circulation for everyone else.
            exec(
                    """
                CREATE UNIQUE INDEX IF NOT EXISTS users_display_name_unique
                ON users (LOWER(display_name))
                WHERE is_deleted = FALSE
            """.trimIndent()
            )

            println("DATABASE: Tables created successfully")
            println("DATABASE: BlockedUsers table should now exist")
        }
    }

    private fun createHikariDataSource(
            url: String,
            user: String,
            password: String
    ): HikariDataSource {
        val config =
                HikariConfig().apply {
                    driverClassName = "org.postgresql.Driver"
                    jdbcUrl = url
                    username = user
                    this.password = password
                    maximumPoolSize = 3
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_READ_COMMITTED"
                    validate()
                }
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
            newSuspendedTransaction(Dispatchers.IO) { block() }
}
