package com.ilkinbayramov.ninjatalk.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseConfig {
    fun initialize() {
        try {
            val serviceAccountStream = FirebaseConfig::class.java.classLoader
                .getResourceAsStream("firebase-adminsdk.json")
                ?: error("firebase-adminsdk.json tapılmadı! resources/ qovluğuna əlavə edilib mi?")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .build()

            FirebaseApp.initializeApp(options)
            println("✅ Firebase Admin SDK initialized successfully")
        } catch (e: Exception) {
            println("❌ Failed to initialize Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
}
