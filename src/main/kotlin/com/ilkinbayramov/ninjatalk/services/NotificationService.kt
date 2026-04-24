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
            
        } catch (e: com.google.firebase.messaging.FirebaseMessagingException) {
            val errorCode = e.messagingErrorCode
            println("❌ FCM MessagingException [$errorCode]: ${e.message}")

            // Stale/invalid token — database-dən sil
            if (errorCode == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED ||
                errorCode == com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT
            ) {
                println("🗑️ FCM: Stale token, database-dən silir (userId=$userId)")
                userService.updateFcmToken(userId, "")
            }
        } catch (e: Exception) {
            println("❌ FCM Exception [${e.javaClass.name}]: ${e.message}")
            var cause: Throwable? = e.cause
            while (cause != null) {
                println("   Caused by [${cause.javaClass.name}]: ${cause.message}")
                cause = cause.cause
            }
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
