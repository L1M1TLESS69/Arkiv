package com.example.utils

import android.content.Context
import com.example.data.DocumentEntity
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object AESCryptUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    private val HEADER_MAGIC = "ARKIV_ENC_V1".toByteArray(Charsets.UTF_8)
    private const val DEFAULT_SECRET = "com.aistudio.secure.vault.arkiv.default.key"

    fun getSecretKey(userPin: String = ""): SecretKeySpec {
        val pinPart = if (userPin.isNotEmpty()) userPin else DEFAULT_SECRET
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(pinPart.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encryptBytes(bytes: ByteArray, userPin: String = ""): ByteArray {
        val keySpec = getSecretKey(userPin)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(bytes)
        // Combine header + encrypted bytes
        val result = ByteArray(HEADER_MAGIC.size + encrypted.size)
        System.arraycopy(HEADER_MAGIC, 0, result, 0, HEADER_MAGIC.size)
        System.arraycopy(encrypted, 0, result, HEADER_MAGIC.size, encrypted.size)
        return result
    }

    fun decryptBytes(bytes: ByteArray, userPin: String = ""): ByteArray {
        if (bytes.size < HEADER_MAGIC.size) return bytes
        
        // Check if magic header exists
        var hasHeader = true
        for (i in HEADER_MAGIC.indices) {
            if (bytes[i] != HEADER_MAGIC[i]) {
                hasHeader = false
                break
            }
        }
        
        if (!hasHeader) {
            // Not encrypted or older version, return as is
            return bytes
        }
        
        // Extract encrypted payload
        val encryptedSize = bytes.size - HEADER_MAGIC.size
        val encrypted = ByteArray(encryptedSize)
        System.arraycopy(bytes, HEADER_MAGIC.size, encrypted, 0, encryptedSize)
        
        val keySpec = getSecretKey(userPin)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(encrypted)
    }

    /**
     * Helper to get a decrypted File copy in cache directory for temporary viewing/rendering
     */
    fun getDecryptedFile(context: Context, localUri: String, userPin: String = ""): File {
        val sourceFile = File(localUri)
        if (!sourceFile.exists()) return sourceFile
        
        try {
            val bytes = sourceFile.readBytes()
            // Check if encrypted
            if (bytes.size >= HEADER_MAGIC.size) {
                var hasHeader = true
                for (i in HEADER_MAGIC.indices) {
                    if (bytes[i] != HEADER_MAGIC[i]) {
                        hasHeader = false
                        break
                    }
                }
                if (hasHeader) {
                    val decryptedBytes = decryptBytes(bytes, userPin)
                    // Create a temp file in cache
                    val cacheFolder = File(context.cacheDir, "decrypted_cache")
                    if (!cacheFolder.exists()) cacheFolder.mkdirs()
                    
                    val tempFile = File(cacheFolder, "view_" + sourceFile.name)
                    tempFile.writeBytes(decryptedBytes)
                    return tempFile
                }
            }
        } catch (e: Exception) {
            // Log/ignore
        }
        return sourceFile
    }

    /**
     * Clear decrypted cache
     */
    fun clearDecryptedCache(context: Context) {
        try {
            val cacheFolder = File(context.cacheDir, "decrypted_cache")
            if (cacheFolder.exists()) {
                cacheFolder.deleteRecursively()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
