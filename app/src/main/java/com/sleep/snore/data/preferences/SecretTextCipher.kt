package com.sleep.snore.data.preferences

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

interface SecretTextCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String?
}

class AndroidKeyStoreSecretTextCipher @Inject constructor() : SecretTextCipher {

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        }
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(1 + cipher.iv.size + encryptedBytes.size)
        payload[0] = cipher.iv.size.toByte()
        cipher.iv.copyInto(payload, destinationOffset = 1)
        encryptedBytes.copyInto(payload, destinationOffset = 1 + cipher.iv.size)
        return Base64.getEncoder().encodeToString(payload)
    }

    override fun decrypt(cipherText: String): String? = runCatching {
        val payload = Base64.getDecoder().decode(cipherText)
        if (payload.isEmpty()) return null
        val ivSize = payload[0].toInt() and 0xFF
        if (ivSize <= 0 || payload.size <= 1 + ivSize) return null
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val encryptedBytes = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        cipher.doFinal(encryptedBytes).toString(Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "sleep_snore_deepseek_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
