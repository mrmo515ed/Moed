package com.animeblack.app.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.animeblack.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private const val AUTH_CALLBACK = "animeblack://auth-callback"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String
)

data class RecoveryTokens(val accessToken: String, val refreshToken: String?)

sealed interface AuthOutcome {
    data class SignedIn(val user: AuthUser) : AuthOutcome
    data class EmailConfirmationRequired(val email: String) : AuthOutcome
}

class AuthException(message: String) : Exception(message)

/** A small Auth REST client using only the project's public publishable key. */
class SupabaseAuthRepository(context: Context) {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val sessionStore = SecureSessionStore(context.applicationContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun signIn(email: String, password: String): AuthOutcome = withContext(Dispatchers.IO) {
        requireConfigured()
        val response = request(
            path = "token?grant_type=password",
            payload = JSONObject().put("email", email.trim()).put("password", password)
        )
        signedInOutcome(response, email)
    }

    suspend fun signUp(
        displayName: String,
        username: String,
        email: String,
        password: String
    ): AuthOutcome = withContext(Dispatchers.IO) {
        requireConfigured()
        val metadata = JSONObject()
            .put("display_name", displayName.trim())
            .put("username", username.trim().removePrefix("@"))
        val response = request(
            path = "signup",
            payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", metadata)
        )
        if (response.optString("access_token").isNotBlank()) {
            signedInOutcome(response, email)
        } else {
            AuthOutcome.EmailConfirmationRequired(email.trim())
        }
    }

    suspend fun sendPasswordReset(email: String) = withContext(Dispatchers.IO) {
        requireConfigured()
        val redirect = Uri.encode(AUTH_CALLBACK)
        request(
            path = "recover?redirect_to=$redirect",
            payload = JSONObject().put("email", email.trim())
        )
    }

    suspend fun updatePassword(tokens: RecoveryTokens, newPassword: String) = withContext(Dispatchers.IO) {
        requireConfigured()
        request(
            path = "user",
            payload = JSONObject().put("password", newPassword),
            accessToken = tokens.accessToken,
            method = "PUT"
        )
        // Store the refreshed session only if the recovery callback included one.
        tokens.refreshToken?.takeIf(String::isNotBlank)?.let { refreshToken ->
            sessionStore.save(refreshToken, tokens.accessToken)
        }
    }

    suspend fun restoreSession(): AuthUser? = withContext(Dispatchers.IO) {
        requireConfigured()
        val saved = sessionStore.read() ?: return@withContext null
        try {
            val refreshed = request(
                path = "token?grant_type=refresh_token",
                payload = JSONObject().put("refresh_token", saved.refreshToken)
            )
            signedInOutcome(refreshed, fallbackEmail = "").let { outcome ->
                (outcome as? AuthOutcome.SignedIn)?.user
            }
        } catch (_: Exception) {
            sessionStore.clear()
            null
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val saved = sessionStore.read()
        try {
            if (saved != null && isConfigured()) {
                request(
                    path = "logout",
                    payload = null,
                    accessToken = saved.accessToken,
                    method = "POST"
                )
            }
        } finally {
            sessionStore.clear()
        }
    }

    private fun signedInOutcome(response: JSONObject, fallbackEmail: String): AuthOutcome {
        val accessToken = response.optString("access_token")
        val refreshToken = response.optString("refresh_token")
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            return AuthOutcome.EmailConfirmationRequired(fallbackEmail)
        }
        sessionStore.save(refreshToken, accessToken)
        val user = response.optJSONObject("user") ?: JSONObject()
        val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
        val email = user.optString("email", fallbackEmail)
        val displayName = metadata.optString("display_name")
            .ifBlank { metadata.optString("full_name") }
            .ifBlank { email.substringBefore('@').ifBlank { "صديق الأنمي" } }
        return AuthOutcome.SignedIn(
            AuthUser(
                id = user.optString("id"),
                email = email,
                displayName = displayName
            )
        )
    }

    private fun request(
        path: String,
        payload: JSONObject?,
        accessToken: String? = null,
        method: String = "POST"
    ): JSONObject {
        val url = "$baseUrl/auth/v1/$path"
        val builder = Request.Builder()
            .url(url)
            .header("apikey", publishableKey)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
        builder.header("Authorization", "Bearer ${accessToken ?: publishableKey}")
        val body = payload?.toString()?.toRequestBody(JSON_MEDIA_TYPE)
        when (method) {
            "PUT" -> builder.put(body ?: "{}".toRequestBody(JSON_MEDIA_TYPE))
            "POST" -> builder.post(body ?: "{}".toRequestBody(JSON_MEDIA_TYPE))
            else -> builder.method(method, body)
        }

        try {
            client.newCall(builder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
                if (!response.isSuccessful) throw AuthException(toArabicError(json, response.code))
                return json
            }
        } catch (error: AuthException) {
            throw error
        } catch (_: SocketTimeoutException) {
            throw AuthException("انتهت مهلة الاتصال. تحقق من الإنترنت وحاول مرة ثانية.")
        } catch (_: Exception) {
            throw AuthException("تعذّر الاتصال بالخادم. تحقق من الإنترنت وحاول مرة ثانية.")
        }
    }

    private fun toArabicError(json: JSONObject, statusCode: Int): String {
        val raw = listOf("msg", "message", "error_description", "error")
            .firstNotNullOfOrNull { key -> json.optString(key).takeIf(String::isNotBlank) }
            ?.lowercase()
            .orEmpty()
        return when {
            "invalid login credentials" in raw || "invalid_credentials" in raw -> "البريد الإلكتروني أو كلمة المرور غير صحيحة."
            "already registered" in raw || "user_already_exists" in raw -> "هذا البريد مسجّل من قبل. جرّب تسجيل الدخول."
            "email not confirmed" in raw -> "تحقق من بريدك الإلكتروني قبل تسجيل الدخول."
            "password" in raw && ("short" in raw || "least" in raw || "length" in raw) -> "كلمة المرور قصيرة؛ استخدم 8 أحرف على الأقل."
            "rate limit" in raw || statusCode == 429 -> "محاولات كثيرة خلال وقت قصير. انتظر قليلًا ثم أعد المحاولة."
            "username" in raw && ("duplicate" in raw || "unique" in raw || "already" in raw) -> "اسم المستخدم مستخدم من قبل. اختر اسمًا آخر."
            else -> "تعذّر إكمال الطلب الآن. تحقق من البيانات وحاول مرة ثانية."
        }
    }

    private fun isConfigured(): Boolean = baseUrl.startsWith("https://") && publishableKey.startsWith("sb_")

    private fun requireConfigured() {
        if (!isConfigured()) {
            throw AuthException("إعداد الاتصال بـSupabase ناقص. أضف رابط المشروع والمفتاح العام في إعدادات البناء.")
        }
    }
}

private data class StoredTokens(val refreshToken: String, val accessToken: String)

/** Stores refresh/access tokens encrypted with a key held by Android Keystore. */
private class SecureSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("anime_black_session", Context.MODE_PRIVATE)
    private val alias = "anime_black_auth_session_v1"

    fun save(refreshToken: String, accessToken: String) {
        runCatching {
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey())
            val clearText = JSONObject()
                .put("refresh", refreshToken)
                .put("access", accessToken)
                .toString()
                .toByteArray(Charsets.UTF_8)
            val encrypted = cipher.doFinal(clearText)
            val packed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "." +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            preferences.edit().putString("encrypted_tokens", packed).apply()
        }
    }

    fun read(): StoredTokens? = runCatching {
        val packed = preferences.getString("encrypted_tokens", null) ?: return null
        val pieces = packed.split('.', limit = 2)
        if (pieces.size != 2) return null
        val iv = Base64.decode(pieces[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(pieces[1], Base64.NO_WRAP)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
        val json = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        StoredTokens(json.getString("refresh"), json.getString("access"))
    }.getOrElse {
        clear()
        null
    }

    fun clear() {
        preferences.edit().remove("encrypted_tokens").apply()
    }

    private fun secretKey(): java.security.Key {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? java.security.Key)?.let { return it }
        val generator = javax.crypto.KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
