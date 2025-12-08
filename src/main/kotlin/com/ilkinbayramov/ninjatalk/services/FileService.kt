package com.ilkinbayramov.ninjatalk.services

import io.ktor.http.content.*
import io.ktor.utils.io.*
import java.io.File
import java.util.*

class FileService {
    private val uploadDir = File("uploads/profile-images")

    init {
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }
    }

    suspend fun saveProfileImage(fileItem: PartData.FileItem, userId: String): String? {
        try {
            println("Saving profile image for user: $userId")
            println("Original filename: ${fileItem.originalFileName}")

            val fileExtension = fileItem.originalFileName?.substringAfterLast('.', "")
            println("File extension: $fileExtension")

            if (fileExtension !in listOf("jpg", "jpeg", "png")) {
                println("Invalid extension: $fileExtension")
                return null
            }

            val fileName = "${userId}_${UUID.randomUUID()}.$fileExtension"
            val file = File(uploadDir, fileName)
            println("Saving to: ${file.absolutePath}")

            // Read from ByteReadChannel and write to file
            val byteArray = fileItem.provider().toByteArray()
            file.writeBytes(byteArray)

            println("File saved successfully: ${file.length()} bytes")
            return "/uploads/profile-images/$fileName"
        } catch (e: Exception) {
            println("Error saving file: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    fun deleteProfileImage(filePath: String): Boolean {
        try {
            val file = File(filePath.removePrefix("/"))
            if (file.exists()) {
                return file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
