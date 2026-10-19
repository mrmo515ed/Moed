package com.animeblack.app

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.animeblack.app.auth.AuthException
import com.animeblack.app.auth.AuthFailureKind
import com.animeblack.app.auth.AuthOutcome
import com.animeblack.app.auth.AuthUser
import com.animeblack.app.auth.RecoveryTokens
import com.animeblack.app.auth.RestoreSessionResult
import com.animeblack.app.auth.SupabaseAuthRepository
import kotlinx.coroutines.launch

private val AppBackground = Color(0xFF090B12)
private val AppBlue = Color(0xFF2D8CFF)
private val AppCyan = Color(0xFF23D8FF)

private data class AuthCallback(
    val type: String,
    val tokens: RecoveryTokens?,
    val errorDescription: String? = null
)

class MainActivity : ComponentActivity() {
    private var authCallback by mutableStateOf<AuthCallback?>(null)
    private lateinit var authRepository: SupabaseAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(9, 11, 18)
        window.navigationBarColor = AndroidColor.rgb(9, 11, 18)
        authRepository = SupabaseAuthRepository(applicationContext)
        consumeAuthCallback(intent)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = AppBlue,
                secondary = AppCyan,
                background = AppBackground,
                surface = Color(0xFF121722),
                onPrimary = Color.White,
                onBackground = Color(0xFFF6F8FF),
                onSurface = Color(0xFFF6F8FF)
            )) {
                AnimeBlackApp(
                    repository = authRepository,
                    callback = authCallback,
                    onCallbackConsumed = { authCallback = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAuthCallback(intent)
    }

    private fun consumeAuthCallback(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme != "animeblack" || uri.host != "auth-callback") return
        val values = LinkedHashMap<String, String>()
        uri.fragment.orEmpty().split('&').forEach { part ->
            val pieces = part.split('=', limit = 2)
            if (pieces.size == 2) values[pieces[0]] = Uri.decode(pieces[1])
        }
        uri.queryParameterNames.forEach { key -> values.putIfAbsent(key, uri.getQueryParameter(key).orEmpty()) }
        val error = values["error_description"] ?: values["error"]
        val token = values["access_token"]
        val type = values["type"].orEmpty()
        authCallback = AuthCallback(
            type = type,
            tokens = token?.takeIf(String::isNotBlank)?.let {
                RecoveryTokens(it, values["refresh_token"]?.takeIf(String::isNotBlank))
            },
            errorDescription = error?.replace('+', ' ')
        )
    }
}

private enum class AuthScreen {
    WELCOME, METHODS, LOGIN, SIGN_UP, VERIFY_EMAIL, EMAIL_SENT, FORGOT, PHONE, OTP,
    RESET_PASSWORD, PROFILE, INTERESTS, SUCCESS, TERMS, PRIVACY, RESTRICTED,
    SESSION_EXPIRED, OFFLINE, HOME
}

private val phoneAuthConfigured = false
private val googleAuthConfigured = false

@androidx.compose.runtime.Composable
private fun AnimeBlackApp(
    repository: SupabaseAuthRepository,
    callback: AuthCallback?,
    onCallbackConsumed: () -> Unit
) {
    var screen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(AuthScreen.WELCOME) }
    var user by androidx.compose.runtime.remember { mutableStateOf<AuthUser?>(null) }
    var busy by androidx.compose.runtime.remember { mutableStateOf(false) }
    var checkingSession by androidx.compose.runtime.remember { mutableStateOf(true) }
    var message by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    var messageIsError by androidx.compose.runtime.remember { mutableStateOf(false) }
    var pendingEmail by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var pendingPhone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var phoneSignup by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var recoveryTokens by androidx.compose.runtime.remember { mutableStateOf<RecoveryTokens?>(null) }
    var retryScreen by androidx.compose.runtime.remember { mutableStateOf(AuthScreen.WELCOME) }
    var retryRestoreSession by androidx.compose.runtime.remember { mutableStateOf(false) }
    var legalReturnScreen by androidx.compose.runtime.remember { mutableStateOf(AuthScreen.WELCOME) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun startTask(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                action()
            } catch (error: AuthException) {
                when (error.kind) {
                    AuthFailureKind.RESTRICTED -> {
                        message = error.message
                        screen = AuthScreen.RESTRICTED
                    }
                    AuthFailureKind.NETWORK -> {
                        if (screen != AuthScreen.OFFLINE) {
                            retryScreen = screen
                            retryRestoreSession = false
                        }
                        message = error.message
                        screen = AuthScreen.OFFLINE
                    }
                    else -> {
                        message = error.message ?: "تعذّر إكمال الطلب. حاول مرة ثانية."
                        messageIsError = true
                    }
                }
            } catch (_: Exception) {
                message = "صار خطأ غير متوقع. حاول مرة ثانية."
                messageIsError = true
            } finally {
                busy = false
            }
        }
    }

    fun runAuth(action: suspend () -> AuthOutcome, afterSignIn: AuthScreen) {
        startTask {
            when (val outcome = action()) {
                is AuthOutcome.SignedIn -> {
                    user = outcome.user
                    if (afterSignIn == AuthScreen.HOME) {
                        repository.loadProfile(outcome.user.id)?.let { profile ->
                            user = outcome.user.copy(
                                displayName = profile.displayName.ifBlank { outcome.user.displayName },
                                username = profile.username,
                                bio = profile.bio,
                                avatarUrl = profile.avatarUrl
                            )
                        }
                    }
                    screen = afterSignIn
                    message = null
                }
                is AuthOutcome.EmailConfirmationRequired -> {
                    pendingEmail = outcome.email
                    screen = AuthScreen.VERIFY_EMAIL
                    message = "أرسلنا رسالة تحقق إلى بريدك. افتح الرابط فيها للمتابعة."
                    messageIsError = false
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (callback != null) {
            checkingSession = false
            return@LaunchedEffect
        }
        when (val restored = repository.restoreSession()) {
            is RestoreSessionResult.Active -> {
                user = restored.user
                screen = AuthScreen.HOME
            }
            RestoreSessionResult.NoSession -> screen = AuthScreen.WELCOME
            RestoreSessionResult.Expired -> screen = AuthScreen.SESSION_EXPIRED
            RestoreSessionResult.Offline -> {
                screen = AuthScreen.OFFLINE
                retryScreen = AuthScreen.WELCOME
                retryRestoreSession = true
            }
        }
        checkingSession = false
    }

    androidx.compose.runtime.LaunchedEffect(callback) {
        val authCallback = callback ?: return@LaunchedEffect
        when {
            authCallback.errorDescription != null -> {
                message = "تعذّر إكمال التحقق. قد يكون الرابط منتهي الصلاحية؛ أعد إرسال رسالة جديدة."
                messageIsError = true
                screen = if (pendingEmail.isNotBlank()) AuthScreen.VERIFY_EMAIL else AuthScreen.LOGIN
            }
            authCallback.type == "recovery" && authCallback.tokens != null -> {
                recoveryTokens = authCallback.tokens
                message = null
                screen = AuthScreen.RESET_PASSWORD
            }
            authCallback.type in setOf("signup", "email", "magiclink") && authCallback.tokens != null -> {
                try {
                    val verifiedUser = repository.completeEmailVerification(authCallback.tokens)
                    user = verifiedUser
                    screen = AuthScreen.PROFILE
                    message = "تم تأكيد بريدك بنجاح. أكمل إعداد ملفك الشخصي."
                    messageIsError = false
                } catch (error: AuthException) {
                    message = error.message
                    messageIsError = true
                    screen = AuthScreen.VERIFY_EMAIL
                }
            }
            else -> {
                message = "رابط التحقق غير مكتمل. افتح الرسالة على هذا الجهاز أو اطلب رابطًا جديدًا."
                messageIsError = true
                screen = AuthScreen.VERIFY_EMAIL
            }
        }
        onCallbackConsumed()
    }

    fun sendRecovery(email: String) {
        startTask {
            repository.sendPasswordReset(email)
            pendingEmail = email.trim()
            message = "إذا كان البريد مرتبطًا بحساب، ستصلك رسالة فيها رابط آمن لإعادة تعيين كلمة المرور."
            messageIsError = false
            screen = AuthScreen.EMAIL_SENT
        }
    }

    fun resendVerification(email: String) {
        startTask {
            repository.resendConfirmation(email)
            message = "أرسلنا رسالة تحقق جديدة. افحص صندوق الوارد والرسائل غير المرغوب فيها."
            messageIsError = false
        }
    }

    fun resetPassword(password: String, confirmation: String) {
        if (password.length < 8) {
            message = "كلمة المرور لازم تكون 8 أحرف على الأقل."
            messageIsError = true
            return
        }
        if (password != confirmation) {
            message = "كلمتا المرور غير متطابقتين."
            messageIsError = true
            return
        }
        val tokens = recoveryTokens ?: run {
            message = "رابط الاستعادة غير صالح أو انتهت صلاحيته. اطلب رابطًا جديدًا."
            messageIsError = true
            return
        }
        startTask {
            repository.updatePassword(tokens, password)
            recoveryTokens = null
            message = "تم تحديث كلمة المرور. سجّل الدخول بكلمتك الجديدة."
            messageIsError = false
            screen = AuthScreen.LOGIN
        }
    }

    fun verifyPhone(code: String) {
        if (code.length != 6) {
            message = "أدخل رمز التحقق المكوّن من 6 أرقام."
            messageIsError = true
            return
        }
        runAuth({ repository.verifyPhoneOtp(pendingPhone, code) }, if (phoneSignup) AuthScreen.PROFILE else AuthScreen.HOME)
    }

    fun saveProfile(displayName: String, username: String, bio: String) {
        val currentUser = user ?: return
        startTask {
            if (!repository.isUsernameAvailable(username, currentUser.id)) {
                throw AuthException("اسم المستخدم مستخدم من قبل. جرّب اسمًا آخر.")
            }
            user = repository.saveProfile(currentUser.id, displayName, username, bio).copy(email = currentUser.email)
            screen = AuthScreen.INTERESTS
            message = null
        }
    }

    fun saveInterests(selectedIds: Set<String>) {
        startTask {
            repository.saveInterests(selectedIds)
            screen = AuthScreen.SUCCESS
        }
    }

    fun retryConnection() {
        startTask {
            if (retryRestoreSession) {
                when (val result = repository.restoreSession()) {
                    is RestoreSessionResult.Active -> { user = result.user; screen = AuthScreen.HOME }
                    RestoreSessionResult.NoSession -> screen = AuthScreen.WELCOME
                    RestoreSessionResult.Expired -> screen = AuthScreen.SESSION_EXPIRED
                    RestoreSessionResult.Offline -> throw AuthException("ما زال الاتصال غير متاح. حاول مرة ثانية.", AuthFailureKind.NETWORK)
                }
            } else {
                screen = retryScreen
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.material3.Surface(color = AppBackground, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(
                    colors = listOf(Color(0xFF102750), AppBackground, AppBackground),
                    center = androidx.compose.ui.geometry.Offset(0.75f, 0.05f),
                    radius = 900f
                ))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            if (checkingSession && callback == null) {
                androidx.compose.material3.CircularProgressIndicator(color = AppCyan)
            } else when (screen) {
                AuthScreen.WELCOME -> WelcomeScreen(
                    onSignUp = { message = null; screen = AuthScreen.SIGN_UP },
                    onLogin = { message = null; screen = AuthScreen.LOGIN },
                    onMethods = { message = null; screen = AuthScreen.METHODS },
                    onTerms = { legalReturnScreen = AuthScreen.WELCOME; screen = AuthScreen.TERMS },
                    onPrivacy = { legalReturnScreen = AuthScreen.WELCOME; screen = AuthScreen.PRIVACY }
                )
                AuthScreen.METHODS -> MethodsScreen(
                    phoneAvailable = phoneAuthConfigured,
                    googleAvailable = googleAuthConfigured,
                    onEmail = { screen = AuthScreen.LOGIN },
                    onPhone = { screen = AuthScreen.PHONE },
                    onGoogle = { message = "Google غير مفعّل في إعدادات Supabase حاليًا."; messageIsError = true },
                    onBack = { screen = AuthScreen.WELCOME }
                )
                AuthScreen.LOGIN -> LoginScreen(
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSubmit = { email, password, remember ->
                        runAuth({ repository.signIn(email, password, remember) }, AuthScreen.HOME)
                    },
                    onForgot = { message = null; screen = AuthScreen.FORGOT },
                    onSignUp = { message = null; screen = AuthScreen.SIGN_UP },
                    onMethods = { message = null; screen = AuthScreen.METHODS },
                    onBack = { message = null; screen = AuthScreen.WELCOME }
                )
                AuthScreen.SIGN_UP -> SignUpScreen(
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSubmit = { displayName, username, email, password ->
                        runAuth({ repository.signUp(displayName, username, email, password) }, AuthScreen.PROFILE)
                    },
                    onCheckUsername = { repository.isUsernameAvailable(it) },
                    onLogin = { message = null; screen = AuthScreen.LOGIN },
                    onTerms = { legalReturnScreen = AuthScreen.SIGN_UP; screen = AuthScreen.TERMS },
                    onPrivacy = { legalReturnScreen = AuthScreen.SIGN_UP; screen = AuthScreen.PRIVACY },
                    onBack = { message = null; screen = AuthScreen.WELCOME }
                )
                AuthScreen.VERIFY_EMAIL -> VerifyEmailScreen(
                    email = pendingEmail,
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onResend = { resendVerification(pendingEmail) },
                    onOpenMail = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL))
                        }.onFailure { message = "افتح تطبيق البريد يدويًا وابحث عن رسالة Anime Black."; messageIsError = false }
                    },
                    onChangeEmail = { screen = AuthScreen.SIGN_UP; message = null },
                    onBack = { screen = AuthScreen.LOGIN; message = null }
                )
                AuthScreen.EMAIL_SENT -> EmailSentScreen(
                    email = pendingEmail,
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onResend = { sendRecovery(pendingEmail) },
                    onChangeEmail = { screen = AuthScreen.FORGOT; message = null },
                    onBack = { screen = AuthScreen.LOGIN; message = null }
                )
                AuthScreen.FORGOT -> ForgotPasswordScreen(
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSubmit = ::sendRecovery,
                    onBack = { message = null; screen = AuthScreen.LOGIN }
                )
                AuthScreen.PHONE -> PhoneScreen(
                    available = phoneAuthConfigured,
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSend = { phone, signup, name, username ->
                        pendingPhone = phone
                        phoneSignup = signup
                        startTask {
                            repository.sendPhoneOtp(phone, signup, name, username)
                            screen = AuthScreen.OTP
                        }
                    },
                    onBack = { screen = AuthScreen.METHODS; message = null }
                )
                AuthScreen.OTP -> OtpScreen(
                    phone = pendingPhone,
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onVerify = ::verifyPhone,
                    onResend = { startTask { repository.sendPhoneOtp(pendingPhone, phoneSignup, "", "") } },
                    onEditPhone = { screen = AuthScreen.PHONE; message = null },
                    onBack = { screen = AuthScreen.PHONE; message = null }
                )
                AuthScreen.RESET_PASSWORD -> ResetPasswordScreen(
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSubmit = ::resetPassword,
                    onBack = { message = null; screen = AuthScreen.LOGIN }
                )
                AuthScreen.PROFILE -> ProfileScreen(
                    user = user,
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSubmit = ::saveProfile,
                    onAvatarSelected = { uri ->
                        val current = user
                        if (current != null) startTask {
                            val avatar = repository.uploadAvatar(current.id, uri)
                            user = current.copy(avatarUrl = avatar)
                            message = "تم تحديث الصورة الشخصية."
                            messageIsError = false
                        }
                    },
                    onBack = { screen = AuthScreen.WELCOME }
                )
                AuthScreen.INTERESTS -> InterestsScreen(
                    repository = repository,
                    userId = user?.id,
                    busy = busy,
                    message = message,
                    messageIsError = messageIsError,
                    onSave = ::saveInterests,
                    onSkip = { screen = AuthScreen.SUCCESS },
                    onBack = { screen = AuthScreen.PROFILE }
                )
                AuthScreen.SUCCESS -> SuccessScreen(
                    name = user?.displayName ?: "صديق الأنمي",
                    onContinue = { screen = AuthScreen.HOME }
                )
                AuthScreen.TERMS -> LegalScreen(title = "شروط الاستخدام", onBack = { screen = legalReturnScreen }, isPrivacy = false)
                AuthScreen.PRIVACY -> LegalScreen(title = "سياسة الخصوصية", onBack = { screen = legalReturnScreen }, isPrivacy = true)
                AuthScreen.RESTRICTED -> StatusScreen(
                    title = "تم تقييد الحساب",
                    body = message ?: "تم تقييد هذا الحساب مؤقتًا بعد رصد نشاط يحتاج إلى مراجعة.",
                    action = "العودة لتسجيل الدخول",
                    onAction = { message = null; screen = AuthScreen.LOGIN }
                )
                AuthScreen.SESSION_EXPIRED -> StatusScreen(
                    title = "انتهت جلستك",
                    body = "انتهت صلاحية جلسة الدخول لحماية حسابك. سجّل الدخول مجددًا للمتابعة.",
                    action = "تسجيل الدخول مجددًا",
                    onAction = { message = null; screen = AuthScreen.LOGIN }
                )
                AuthScreen.OFFLINE -> StatusScreen(
                    title = "تعذّر الاتصال",
                    body = message ?: "يبدو أنك غير متصل بالإنترنت أو أن الخادم لا يستجيب الآن.",
                    action = "إعادة المحاولة",
                    onAction = ::retryConnection,
                    secondaryAction = "العودة للبداية",
                    onSecondary = { message = null; screen = retryScreen }
                )
                AuthScreen.HOME -> HomeScreen(
                    user = user,
                    busy = busy,
                    onSignOut = {
                        startTask {
                            repository.signOut()
                            user = null
                            message = null
                            screen = AuthScreen.WELCOME
                        }
                    },
                    onEditProfile = { screen = AuthScreen.PROFILE }
                )
            }
        }
    }
}
