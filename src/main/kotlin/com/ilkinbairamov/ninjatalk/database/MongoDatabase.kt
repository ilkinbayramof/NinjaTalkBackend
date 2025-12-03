package com.ilkinbairamov.ninjatalk.database

import com.ilkinbairamov.ninjatalk.models.User
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import java.util.concurrent.TimeUnit
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

object MongoDatabase {
    private lateinit var connectionString: String

    fun init(uri: String) {
        connectionString = uri
    }

    private val client by lazy {
        val settings =
                MongoClientSettings.builder()
                        .applyConnectionString(ConnectionString(connectionString))
                        .applyToSocketSettings { builder ->
                            builder.connectTimeout(10, TimeUnit.SECONDS)
                            builder.readTimeout(10, TimeUnit.SECONDS)
                        }
                        .applyToSslSettings { builder ->
                            builder.enabled(true)
                            builder.invalidHostNameAllowed(true)
                        }
                        .build()

        KMongo.createClient(settings).coroutine
    }

    private val database by lazy { client.getDatabase("ninjatalk") }

    val users by lazy { database.getCollection<User>("users") }
}
