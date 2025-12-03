package com.ilkinbairamov.ninjatalk.database

import com.ilkinbairamov.ninjatalk.models.User
import com.mongodb.ConnectionString
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

object MongoDatabase {
    private lateinit var connectionString: String

    fun init(uri: String) {
        connectionString = uri
    }

    private val client by lazy { KMongo.createClient(ConnectionString(connectionString)).coroutine }

    private val database by lazy { client.getDatabase("ninjatalk") }

    val users by lazy { database.getCollection<User>("users") }
}
