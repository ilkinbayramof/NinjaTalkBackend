package com.ilkinbayramov.ninjatalk.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseConfig {
    fun initialize() {
        try {
            val serviceAccount = FileInputStream("src/main/resources/firebase-adminsdk.json")
            
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()
            
            FirebaseApp.initializeApp(options)
            println("✅ Firebase Admin SDK initialized successfully")
        } catch (e: Exception) {
            println("❌ Failed to initialize Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
}
