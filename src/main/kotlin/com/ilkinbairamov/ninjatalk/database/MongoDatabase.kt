package com.ilkinbairamov.ninjatalk.database

import com.ilkinbairamov.ninjatalk.models.User
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

object MongoDatabase {
    private lateinit var connectionString: String

    fun init(uri: String) {
        connectionString = uri
    }

    private fun createTrustAllSslContext(): SSLContext {
        val trustAllCerts =
                arrayOf<TrustManager>(
                        object : X509TrustManager {
                            override fun checkClientTrusted(
                                    chain: Array<X509Certificate>,
                                    authType: String
                            ) {}
                            override fun checkServerTrusted(
                                    chain: Array<X509Certificate>,
                                    authType: String
                            ) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }
                )

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext
    }

    private val client by lazy {
        val settings =
                MongoClientSettings.builder()
                        .applyConnectionString(ConnectionString(connectionString))
                        .applyToSocketSettings { builder ->
                            builder.connectTimeout(30, TimeUnit.SECONDS)
                            builder.readTimeout(30, TimeUnit.SECONDS)
                        }
                        .applyToSslSettings { builder ->
                            builder.enabled(true)
                            builder.invalidHostNameAllowed(true)
                            builder.context(createTrustAllSslContext())
                        }
                        .build()

        KMongo.createClient(settings).coroutine
    }

    private val database by lazy { client.getDatabase("ninjatalk") }

    val users by lazy { database.getCollection<User>("users") }
}
