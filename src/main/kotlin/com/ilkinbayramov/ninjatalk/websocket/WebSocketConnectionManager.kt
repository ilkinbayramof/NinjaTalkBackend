package com.ilkinbayramov.ninjatalk.websocket

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

object WebSocketConnectionManager {
    // userId -> WebSocketSession mapping
    private val connections = ConcurrentHashMap<String, WebSocketSession>()

    fun addConnection(userId: String, session: WebSocketSession) {
        connections[userId] = session
        println("WS: User $userId connected. Total connections: ${connections.size}")
    }

    fun removeConnection(userId: String) {
        connections.remove(userId)
        println("WS: User $userId disconnected. Total connections: ${connections.size}")
    }

    suspend fun sendToUser(userId: String, message: String) {
        connections[userId]?.send(Frame.Text(message))
    }

    suspend fun sendToUsers(userIds: List<String>, message: String) {
        userIds.forEach { userId -> sendToUser(userId, message) }
    }

    fun isUserOnline(userId: String): Boolean = connections.containsKey(userId)

    fun getActiveConnections(): Int = connections.size
}
