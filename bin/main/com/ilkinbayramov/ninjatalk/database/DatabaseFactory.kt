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
            SchemaUtils.create(Users, Conversations, Messages, AnonymousIdentities, BlockedUsers)

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
