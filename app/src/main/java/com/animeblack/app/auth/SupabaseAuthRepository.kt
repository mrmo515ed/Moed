package com.animeblack.app.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.animeblack.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private const val AUTH_CALLBACK = "animeblack://auth-callback"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

enum class AuthFailureKind { GENERAL, NETWORK, RESTRICTED, CONFIGURATION, PROVIDER_DISABLED }

class AuthException(message: String, val kind: AuthFailureKind = AuthFailureKind.GENERAL) : Exception(message)

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String? = null
)

data class RecoveryTokens(val accessToken: String, val refreshToken: String?)

data class AnimeGenre(val id: String, val slug: String, val nameArabic: String, val nameEnglish: String)

sealed interface AuthOutcome {
    data class SignedIn(val user: AuthUser) : AuthOutcome
    data class EmailConfirmationRequired(val email: String) : AuthOutcome
}

sealed interface RestoreSessionResult {
    data object NoSession : RestoreSessionResult
    data class Active(val user: AuthUser) : RestoreSessionResult
    data object Expired : RestoreSessionResult
    data object Offline : RestoreSessionResult
}

/** Supabase Auth + PostgREST + Storage client. The app only uses the public publishable key. */
class SupabaseAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val sessionStore = SecureSessionStore(appContext)
    @Volatile private var activeTokens: StoredTokens? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    suspend fun signIn(email: String, password: String, rememberMe: Boolean): AuthOutcome {
        requireConfigured()
        val response = authJson(
            path = "token?grant_type=password",
            payload = JSONObject().put("email", email.trim()).put("password", password)
        )
        return signedInOutcome(response, email, rememberMe)
    }

    suspend fun signUp(displayName: String, username: String, email: String, password: String): AuthOutcome {
        requireConfigured()
        val metadata = JSONObject()
            .put("display_name", displayName.trim())
            .put("username", username.trim().removePrefix("@"))
        val redirect = Uri.encode(AUTH_CALLBACK)
        val response = authJson(
            path = "signup?redirect_to=$redirect",
            payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", metadata)
        )
        return if (response.optString("access_token").isNotBlank()) {
            signedInOutcome(response, email, rememberMe = true)
        } else {
            AuthOutcome.EmailConfirmationRequired(email.trim())
        }
    }

    suspend fun resendConfirmation(email: String) {
        requireConfigured()
        val redirect = Uri.encode(AUTH_CALLBACK)
        authJson(
            path = "resend?redirect_to=$redirect",
            payload = JSONObject().put("type", "signup").put("email", email.trim())
        )
    }

    suspend fun sendPasswordReset(email: String) {
        requireConfigured()
        val redirect = Uri.encode(AUTH_CALLBACK)
        authJson(
            path = "recover?redirect_to=$redirect",
            payload = JSONObject().put("email", email.trim())
        )
    }

    suspend fun completeEmailVerification(tokens: RecoveryTokens): AuthUser {
        requireConfigured()
        val token = tokens.refreshToken ?: throw AuthException("رابط التحقق غير مكتمل. اطلب رسالة جديدة.")
        val json = authJson("user", method = "GET", accessToken = tokens.accessToken)
        activeTokens = StoredTokens(token, tokens.accessToken)
        sessionStore.save(token, tokens.accessToken)
        return userFrom(json)
    }

    suspend fun updatePassword(tokens: RecoveryTokens, newPassword: String) {
        requireConfigured()
        authJson(
            path = "user",
            payload = JSONObject().put("password", newPassword),
            accessToken = tokens.accessToken,
            method = "PUT"
        )
        tokens.refreshToken?.takeIf(String::isNotBlank)?.let {
            activeTokens = StoredTokens(it, tokens.accessToken)
            sessionStore.save(it, tokens.accessToken)
        }
        activeTokens = null
        sessionStore.clear()
    }

    suspend fun sendPhoneOtp(phone: String, createUser: Boolean, displayName: String, username: String) {
        requireConfigured()
        val body = JSONObject().put("phone", phone).put("create_user", createUser)
        if (createUser) {
            body.put("data", JSONObject()
                .put("display_name", displayName.trim())
                .put("username", username.trim().removePrefix("@")))
        }
        authJson("otp", payload = body)
    }

    suspend fun verifyPhoneOtp(phone: String, code: String): AuthOutcome {
        requireConfigured()
        val response = authJson(
            "verify",
            payload = JSONObject().put("phone", phone).put("token", code).put("type", "sms")
        )
        return signedInOutcome(response, fallbackEmail = "", rememberMe = true)
    }

    suspend fun restoreSession(): RestoreSessionResult {
        requireConfigured()
        val saved = sessionStore.read() ?: return RestoreSessionResult.NoSession
        return try {
            val refreshed = authJson(
                path = "token?grant_type=refresh_token",
                payload = JSONObject().put("refresh_token", saved.refreshToken)
            )
            when (val outcome = signedInOutcome(refreshed, fallbackEmail = "", rememberMe = true)) {
                is AuthOutcome.SignedIn -> RestoreSessionResult.Active(outcome.user)
                is AuthOutcome.EmailConfirmationRequired -> RestoreSessionResult.Expired
            }
        } catch (error: AuthException) {
            sessionStore.clear()
            activeTokens = null
            if (error.kind == AuthFailureKind.NETWORK) RestoreSessionResult.Offline else RestoreSessionResult.Expired
        }
    }

    suspend fun signOut() {
        val saved = currentTokens()
        try {
            if (saved != null && isConfigured()) {
                authJson("logout", accessToken = saved.accessToken)
            }
        } finally {
            activeTokens = null
            sessionStore.clear()
        }
    }

    suspend fun loadProfile(userId: String): AuthUser? {
        val url = "$baseUrl/rest/v1/profiles?id=eq.${Uri.encode(userId)}&select=id,username,display_name,bio,avatar_url&limit=1"
        val response = restRaw(url, method = "GET")
        val array = JSONArray(response)
        if (array.length() == 0) return null
        val profile = array.getJSONObject(0)
        return AuthUser(
            id = profile.optString("id", userId),
            email = "",
            displayName = profile.optString("display_name"),
            username = profile.optString("username"),
            bio = profile.optString("bio"),
            avatarUrl = profile.optString("avatar_url").takeIf(String::isNotBlank)
        )
    }

    suspend fun isUsernameAvailable(username: String, currentUserId: String? = null): Boolean {
        val value = Uri.encode(username.trim().removePrefix("@"))
        val url = "$baseUrl/rest/v1/profiles?username=eq.$value&select=id&limit=1"
        val array = JSONArray(restRaw(url, method = "GET"))
        if (array.length() == 0) return true
        return currentUserId != null && array.optJSONObject(0)?.optString("id") == currentUserId
    }

    suspend fun saveProfile(userId: String, displayName: String, username: String, bio: String): AuthUser {
        val payload = JSONObject()
            .put("display_name", displayName.trim())
            .put("username", username.trim().removePrefix("@"))
            .put("bio", bio.trim())
        val url = "$baseUrl/rest/v1/profiles?id=eq.${Uri.encode(userId)}"
        val response = restRaw(url, "PATCH", payload.toString(), prefer = "return=representation")
        val saved = JSONArray(response).optJSONObject(0)
            ?: throw AuthException("لم نتمكن من حفظ الملف الشخصي. حاول مرة أخرى.")
        return AuthUser(
            id = saved.optString("id", userId),
            email = "",
            displayName = saved.optString("display_name", displayName),
            username = saved.optString("username", username),
            bio = saved.optString("bio", bio),
            avatarUrl = saved.optString("avatar_url").takeIf(String::isNotBlank)
        )
    }

    suspend fun uploadAvatar(userId: String, imageUri: Uri): String {
        val jpeg = withContext(Dispatchers.IO) { compressAvatar(imageUri) }
        val path = "${Uri.encode(userId)}/avatar.jpg"
        val url = "$baseUrl/storage/v1/object/avatars/$path"
        restRaw(
            url = url,
            method = "POST",
            bodyBytes = jpeg,
            contentType = "image/jpeg",
            extraHeaders = mapOf("x-upsert" to "true")
        )
        val publicUrl = "$baseUrl/storage/v1/object/public/avatars/$path?v=${System.currentTimeMillis()}"
        val saved = currentTokens() ?: throw AuthException("انتهت الجلسة. سجّل الدخول من جديد.")
        restRaw(
            url = "$baseUrl/rest/v1/profiles?id=eq.${Uri.encode(userId)}",
            method = "PATCH",
            rawBody = JSONObject().put("avatar_url", publicUrl).toString(),
            prefer = "return=minimal",
            accessToken = saved.accessToken
        )
        return publicUrl
    }

    suspend fun loadGenres(): List<AnimeGenre> {
        val response = restRaw(
            "$baseUrl/rest/v1/genres?select=id,slug,name_ar,name_en&order=slug.asc",
            method = "GET"
        )
        val rows = JSONArray(response)
        return (0 until rows.length()).mapNotNull { index ->
            rows.optJSONObject(index)?.let { row ->
                AnimeGenre(
                    id = row.optString("id"),
                    slug = row.optString("slug"),
                    nameArabic = row.optString("name_ar"),
                    nameEnglish = row.optString("name_en")
                )
            }
        }
    }

    suspend fun loadMyInterests(userId: String): Set<String> {
        val url = "$baseUrl/rest/v1/profile_genre_interests?user_id=eq.${Uri.encode(userId)}&select=genre_id"
        val array = JSONArray(restRaw(url, method = "GET"))
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("genre_id") }.toSet()
    }

    suspend fun saveInterests(genreIds: Set<String>) {
        if (genreIds.size < 3) throw AuthException("اختر 3 اهتمامات على الأقل.")
        if (genreIds.size > 10) throw AuthException("اختر 10 اهتمامات كحد أقصى.")
        val payload = JSONObject().put("p_genre_ids", JSONArray(genreIds.toList()))
        restRaw("$baseUrl/rest/v1/rpc/replace_my_genre_interests", "POST", payload.toString())
    }

    private suspend fun signedInOutcome(response: JSONObject, fallbackEmail: String, rememberMe: Boolean): AuthOutcome {
        val accessToken = response.optString("access_token")
        val refreshToken = response.optString("refresh_token")
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            activeTokens = null
            if (rememberMe) sessionStore.clear()
            return AuthOutcome.EmailConfirmationRequired(fallbackEmail)
        }
        val session = StoredTokens(refreshToken, accessToken)
        activeTokens = session
        if (rememberMe) sessionStore.save(refreshToken, accessToken) else sessionStore.clear()
        val user = response.optJSONObject("user") ?: JSONObject()
        return AuthOutcome.SignedIn(userFrom(user))
    }

    private fun userFrom(user: JSONObject): AuthUser {
        val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
        val email = user.optString("email")
        return AuthUser(
            id = user.optString("id"),
            email = email,
            displayName = metadata.optString("display_name")
                .ifBlank { metadata.optString("full_name") }
                .ifBlank { email.substringBefore('@').ifBlank { "صديق الأنمي" } },
            username = metadata.optString("username"),
            avatarUrl = metadata.optString("avatar_url").takeIf(String::isNotBlank)
        )
    }

    internal suspend fun socialRest(
        path: String,
        method: String = "GET",
        body: String? = null,
        prefer: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String = restRaw(
        url = "$baseUrl/rest/v1/${path.trimStart('/')}",
        method = method,
        rawBody = body,
        prefer = prefer,
        extraHeaders = extraHeaders
    )

    internal suspend fun uploadSocialObject(bucket: String, path: String, bytes: ByteArray, contentType: String): String {
        restRaw(
            url = "$baseUrl/storage/v1/object/$bucket/$path",
            method = "POST",
            bodyBytes = bytes,
            contentType = contentType,
            extraHeaders = mapOf("x-upsert" to "false")
        )
        return "$baseUrl/storage/v1/object/public/$bucket/$path"
    }

    private suspend fun authJson(
        path: String,
        payload: JSONObject? = null,
        accessToken: String? = null,
        method: String = "POST"
    ): JSONObject {
        val body = payload?.toString()
        val raw = requestRaw(
            url = "$baseUrl/auth/v1/$path",
            method = method,
            rawBody = body,
            accessToken = accessToken
        )
        return if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    private suspend fun restRaw(
        url: String,
        method: String,
        rawBody: String? = null,
        bodyBytes: ByteArray? = null,
        contentType: String = "application/json; charset=utf-8",
        prefer: String? = null,
        accessToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String = requestRaw(
        url = url,
        method = method,
        rawBody = rawBody,
        bodyBytes = bodyBytes,
        contentType = contentType,
        prefer = prefer,
        accessToken = accessToken ?: currentTokens()?.accessToken,
        extraHeaders = extraHeaders
    )

    private suspend fun requestRaw(
        url: String,
        method: String,
        rawBody: String? = null,
        bodyBytes: ByteArray? = null,
        contentType: String = "application/json; charset=utf-8",
        prefer: String? = null,
        accessToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        requireConfigured()
        val token = accessToken ?: publishableKey
        val requestBuilder = Request.Builder()
            .url(url)
            .header("apikey", publishableKey)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
        prefer?.let { requestBuilder.header("Prefer", it) }
        extraHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }
        val requestBody = when {
            bodyBytes != null -> bodyBytes.toRequestBody(contentType.toMediaType())
            rawBody != null -> rawBody.toRequestBody(contentType.toMediaType())
            else -> null
        }
        when (method) {
            "POST" -> requestBuilder.post(requestBody ?: ByteArray(0).toRequestBody(contentType.toMediaType()))
            "PUT" -> requestBuilder.put(requestBody ?: ByteArray(0).toRequestBody(contentType.toMediaType()))
            "PATCH" -> requestBuilder.patch(requestBody ?: ByteArray(0).toRequestBody(contentType.toMediaType()))
            "DELETE" -> requestBuilder.delete(requestBody)
            else -> requestBuilder.method(method, requestBody)
        }
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw parseError(responseText, response.code)
                responseText
            }
        } catch (error: AuthException) {
            throw error
        } catch (_: SocketTimeoutException) {
            throw AuthException("انتهت مهلة الاتصال. تحقق من الإنترنت وحاول مرة ثانية.", AuthFailureKind.NETWORK)
        } catch (_: IOException) {
            throw AuthException("تعذّر الاتصال بالخادم. تحقق من الإنترنت وحاول مرة ثانية.", AuthFailureKind.NETWORK)
        } catch (_: Exception) {
            throw AuthException("صار خطأ غير متوقع أثناء الاتصال. حاول مرة ثانية.", AuthFailureKind.NETWORK)
        }
    }

    private fun parseError(raw: String, statusCode: Int): AuthException {
        val json = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        val detail = listOf("msg", "message", "error_description", "error", "hint")
            .firstNotNullOfOrNull { key -> json.optString(key).takeIf(String::isNotBlank) }
            .orEmpty()
        val lowered = detail.lowercase()
        val kind = when {
            "banned" in lowered || "suspended" in lowered || "restricted" in lowered -> AuthFailureKind.RESTRICTED
            "unsupported provider" in lowered || "phone provider is not enabled" in lowered || "sms provider" in lowered -> AuthFailureKind.PROVIDER_DISABLED
            statusCode == 429 -> AuthFailureKind.GENERAL
            else -> AuthFailureKind.GENERAL
        }
        val message = when {
            kind == AuthFailureKind.RESTRICTED -> "تم تقييد الحساب مؤقتًا. تواصل مع الدعم إذا كنت تعتقد أن هذا خطأ."
            kind == AuthFailureKind.PROVIDER_DISABLED -> "طريقة الدخول هذه غير مفعّلة في إعدادات المشروع بعد."
            "invalid login credentials" in lowered || "invalid_credentials" in lowered -> "البريد الإلكتروني أو كلمة المرور غير صحيحة."
            "already registered" in lowered || "user_already_exists" in lowered -> "هذا البريد مسجّل من قبل. جرّب تسجيل الدخول."
            "email not confirmed" in lowered -> "أكد بريدك الإلكتروني أولًا من رسالة التحقق."
            "username" in lowered && ("duplicate" in lowered || "unique" in lowered || "already" in lowered) -> "اسم المستخدم مستخدم من قبل. اختر اسمًا آخر."
            "password" in lowered && ("short" in lowered || "least" in lowered || "length" in lowered) -> "كلمة المرور قصيرة؛ استخدم 8 أحرف على الأقل."
            statusCode == 429 -> "محاولات كثيرة خلال وقت قصير. انتظر قليلًا ثم أعد المحاولة."
            else -> "تعذّر إكمال الطلب الآن. تحقق من البيانات وحاول مرة ثانية."
        }
        return AuthException(message, kind)
    }

    private fun compressAvatar(uri: Uri): ByteArray {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw AuthException("تعذّر قراءة الصورة المحددة.")
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw AuthException("تعذّر فتح الصورة المحددة.")
        val side = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - side) / 2
        val y = (bitmap.height - side) / 2
        val square = Bitmap.createBitmap(bitmap, x, y, side, side)
        val resized = Bitmap.createScaledBitmap(square, 512, 512, true)
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 86, output)
        if (resized !== bitmap) resized.recycle()
        if (square !== bitmap) square.recycle()
        if (!bitmap.isRecycled) bitmap.recycle()
        return output.toByteArray()
    }

    private fun currentTokens(): StoredTokens? = activeTokens ?: sessionStore.read()

    private fun isConfigured(): Boolean = baseUrl.startsWith("https://") && publishableKey.startsWith("sb_")

    private fun requireConfigured() {
        if (!isConfigured()) {
            throw AuthException("إعداد الاتصال بـSupabase ناقص. أضف رابط المشروع والمفتاح العام لإعدادات البناء.", AuthFailureKind.CONFIGURATION)
        }
    }
}

private data class StoredTokens(val refreshToken: String, val accessToken: String)

/** Stores tokens encrypted with a key protected by Android Keystore. */
private class SecureSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("anime_black_session", Context.MODE_PRIVATE)
    private val alias = "anime_black_auth_session_v1"

    fun save(refreshToken: String, accessToken: String) {
        runCatching {
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey())
            val clearText = JSONObject().put("refresh", refreshToken).put("access", accessToken)
                .toString().toByteArray(Charsets.UTF_8)
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
