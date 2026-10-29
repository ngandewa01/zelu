package com.koolicream.zelu

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Password-protected backups. The file holds AES-256-GCM ciphertext; the key comes from your password
// (PBKDF2-HMAC-SHA256, random salt), so the file is unreadable without it. Zelu cannot recover a forgotten password.
object BackupCrypto {
    private const val ITER = 150_000
    const val MIN_PASSWORD = 6

    fun isEncrypted(text: String): Boolean = try { JSONObject(text).optInt("enc", 0) == 1 } catch (_: Exception) { false }

    // PBKDF2 with HMAC-SHA256, written out so it works on every Android version Zelu supports.
    internal fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, bytes: Int): ByteArray {
        val pw = String(password).toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(pw, "HmacSHA256"))
        val out = ByteArray(bytes)
        var block = 1
        var pos = 0
        while (pos < bytes) {
            mac.update(salt)
            mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 1 until iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val n = minOf(t.size, bytes - pos)
            System.arraycopy(t, 0, out, pos, n)
            pos += n; block++
        }
        return out
    }

    fun encrypt(plain: String, password: String): String {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val key = SecretKeySpec(pbkdf2(password.toCharArray(), salt, ITER, 32), "AES")
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val data = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return JSONObject().put("app", "zelu").put("enc", 1).put("iter", ITER)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(data, Base64.NO_WRAP)).toString()
    }

    // Returns the plain text, or null when the password is wrong or the file is damaged.
    fun decrypt(text: String, password: String): String? = try {
        val o = JSONObject(text)
        val salt = Base64.decode(o.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(o.getString("iv"), Base64.NO_WRAP)
        val data = Base64.decode(o.getString("data"), Base64.NO_WRAP)
        val key = SecretKeySpec(pbkdf2(password.toCharArray(), salt, o.optInt("iter", ITER), 32), "AES")
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        String(c.doFinal(data), Charsets.UTF_8)
    } catch (_: Exception) { null }
}

// Keeps the backup password on this phone, locked by the Android Keystore, so the weekly backup can be
// encrypted without asking each time. The password never leaves the phone; a new phone needs it typed in.
object BackupVault {
    private const val ALIAS = "zelu_backup_key"
    private const val PREFS = "zelu_backup"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return g.generateKey()
    }

    fun has(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("pw")

    fun save(context: Context, password: String) {
        try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key())
            val enc = c.doFinal(password.toByteArray(Charsets.UTF_8))
            val v = Base64.encodeToString(c.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(enc, Base64.NO_WRAP)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("pw", v).apply()
        } catch (e: Exception) { CrashLog.warn("BackupCrypto.save", e) }
    }

    fun load(context: Context): String? = try {
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("pw", null)
        if (v == null) null else {
            val (iv, data) = v.split(":")
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(c.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        }
    } catch (_: Exception) { null }
}
