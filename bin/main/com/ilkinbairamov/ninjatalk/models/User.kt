package com.ilkinbairamov.ninjatalk.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class User(
        @BsonId val id: String = ObjectId().toString(),
        val email: String,
        val passwordHash: String,
        val gender: String, // "MALE" or "FEMALE"
        val birthDate: String, // ISO 8601 format: "2000-01-15"
        val createdAt: Long = System.currentTimeMillis()
)
