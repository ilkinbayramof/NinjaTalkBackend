package com.ilkinbayramov.ninjatalk.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

class NotificationService(private val userService: UserService) {
    
    suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        try {
            // Get user's FCM token
            val user = userService.getUserById(userId)
            val fcmToken = user?.fcmToken
            
            if (fcmToken.isNullOrEmpty()) {
                println("⚠️ FCM: No token for user $userId")
                return
            }
            
            // Build FCM message
            val notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()
            
            val messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(notification)
            
            // Add custom data
            data.forEach { (key, value) ->
                messageBuilder.putData(key, value)
            }
            
            val message = messageBuilder.build()
            
            // Send notification
            val response = FirebaseMessaging.getInstance().send(message)
            println("🔔 FCM: Sent notification to $userId - response: $response")
            
        } catch (e: Exception) {
            println("❌ FCM: Failed to send notification - ${e.message}")
            e.printStackTrace()
        }
    }
    
    suspend fun sendMessageNotification(
        recipientId: String,
        senderName: String,
        messageContent: String,
        conversationId: String
    ) {
        sendPushNotification(
            userId = recipientId,
            title = "Yeni Mesaj",
            body = senderName,
            data = mapOf(
                "type" to "new_message",
                "conversationId" to conversationId
            )
        )
    }
}
