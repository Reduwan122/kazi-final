package com.example.data.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH_BIT = 256

    data class EncryptionResult(
        val cipherTextBase64: String,
        val saltBase64: String,
        val ivBase64: String
    )

    fun encrypt(plainText: String, password: String): EncryptionResult {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTE)
        random.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTE)
        random.nextBytes(iv)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptionResult(
            cipherTextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(cipherTextBase64: String, saltBase64: String, ivBase64: String, password: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}

