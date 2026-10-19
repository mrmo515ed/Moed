package com.animeblack.app

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animeblack.app.auth.AuthException
import com.animeblack.app.auth.AuthOutcome
import com.animeblack.app.auth.AuthUser
import com.animeblack.app.auth.RecoveryTokens
import com.animeblack.app.auth.SupabaseAuthRepository
import kotlinx.coroutines.launch

private val Background = Color(0xFF090B12)
private val Panel = Color(0xFF121722)
private val PanelLight = Color(0xFF171E2D)
private val PrimaryBlue = Color(0xFF2D8CFF)
private val Cyan = Color(0xFF23D8FF)
private val MainText = Color(0xFFF6F8FF)
private val MutedText = Color(0xFF98A3BA)
private val ErrorRed = Color(0xFFFF718A)
private val SuccessGreen = Color(0xFF4FE0A9)

class MainActivity : ComponentActivity() {
    private var recoveryLink by mutableStateOf<RecoveryTokens?>(null)
    private lateinit var authRepository: SupabaseAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(9, 11, 18)
        window.navigationBarColor = AndroidColor.rgb(9, 11, 18)
        authRepository = SupabaseAuthRepository(applicationContext)
        consumeAuthCallback(intent)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
                primary = PrimaryBlue,
                secondary = Cyan,
                background = Background,
                surface = Panel,
                onPrimary = Color.White,
                onBackground = MainText,
                onSurface = MainText
            )) {
                AnimeBlackApp(
                    repository = authRepository,
                    initialRecovery = recoveryLink
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
        val params = uri.fragment.orEmpty().split('&')
            .mapNotNull { part ->
                val pieces = part.split('=', limit = 2)
                if (pieces.size == 2) pieces[0] to Uri.decode(pieces[1]) else null
            }.toMap()
        val accessToken = params["access_token"].orEmpty()
        if (accessToken.isNotBlank() && params["type"] == "recovery") {
            recoveryLink = RecoveryTokens(accessToken, params["refresh_token"])
        }
    }
}

private enum class AuthScreen { WELCOME, LOGIN, SIGN_UP, FORGOT, RESET_PASSWORD, HOME }

@Composable
private fun AnimeBlackApp(
    repository: SupabaseAuthRepository,
    initialRecovery: RecoveryTokens?
) {
    var screen by rememberSaveable { mutableStateOf(AuthScreen.WELCOME) }
    var user by remember { mutableStateOf<AuthUser?>(null) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var recoveryTokens by remember { mutableStateOf(initialRecovery) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialRecovery) {
        if (initialRecovery != null) {
            recoveryTokens = initialRecovery
            screen = AuthScreen.RESET_PASSWORD
            message = null
        }
    }

    LaunchedEffect(repository) {
        try {
            user = repository.restoreSession()
            if (user != null && initialRecovery == null) screen = AuthScreen.HOME
        } catch (_: Exception) {
            user = null
        } finally {
            isCheckingSession = false
        }
    }

    fun runAuth(action: suspend () -> AuthOutcome) {
        if (isBusy) return
        isBusy = true
        message = null
        scope.launch {
            try {
                when (val result = action()) {
                    is AuthOutcome.SignedIn -> {
                        user = result.user
                        screen = AuthScreen.HOME
                        message = null
                    }
                    is AuthOutcome.EmailConfirmationRequired -> {
                        screen = AuthScreen.LOGIN
                        message = "أنشأنا الحساب. افحص بريدك الإلكتروني لتأكيده، ثم سجّل الدخول."
                        messageIsError = false
                    }
                }
            } catch (error: AuthException) {
                message = error.message ?: "تعذّر إكمال الطلب. حاول مرة ثانية."
                messageIsError = true
            } catch (_: Exception) {
                message = "صار خطأ غير متوقع. حاول مرة ثانية."
                messageIsError = true
            } finally {
                isBusy = false
            }
        }
    }

    fun sendReset(email: String) {
        if (isBusy) return
        isBusy = true
        message = null
        scope.launch {
            try {
                repository.sendPasswordReset(email)
                message = "إذا كان البريد مسجّلًا، سيصلك رابط آمن لإعادة تعيين كلمة المرور."
                messageIsError = false
            } catch (error: AuthException) {
                message = error.message ?: "تعذّر إرسال رسالة الاستعادة."
                messageIsError = true
            } finally {
                isBusy = false
            }
        }
    }

    fun saveNewPassword(password: String, confirmPassword: String) {
        if (password.length < 8) {
            message = "استخدم 8 أحرف على الأقل لكلمة المرور."
            messageIsError = true
            return
        }
        if (password != confirmPassword) {
            message = "كلمتا المرور غير متطابقتين."
            messageIsError = true
            return
        }
        val tokens = recoveryTokens
        if (tokens == null) {
            message = "رابط الاستعادة غير صالح أو انتهت صلاحيته. اطلب رابطًا جديدًا."
            messageIsError = true
            return
        }
        isBusy = true
        message = null
        scope.launch {
            try {
                repository.updatePassword(tokens, password)
                recoveryTokens = null
                message = "تم تحديث كلمة المرور. سجّل الدخول بكلمتك الجديدة."
                messageIsError = false
                screen = AuthScreen.LOGIN
            } catch (error: AuthException) {
                message = error.message ?: "تعذّر تحديث كلمة المرور. اطلب رابطًا جديدًا."
                messageIsError = true
            } finally {
                isBusy = false
            }
        }
    }

    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF102750), Background, Background),
                        center = androidx.compose.ui.geometry.Offset(0.75f, 0.05f),
                        radius = 900f
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            if (isCheckingSession && initialRecovery == null) {
                CircularProgressIndicator(color = Cyan)
            } else {
                when (screen) {
                    AuthScreen.WELCOME -> WelcomeScreen(
                        onSignUp = { message = null; screen = AuthScreen.SIGN_UP },
                        onLogin = { message = null; screen = AuthScreen.LOGIN }
                    )
                    AuthScreen.LOGIN -> LoginScreen(
                        busy = isBusy,
                        message = message,
                        messageIsError = messageIsError,
                        onSubmit = { email, password -> runAuth { repository.signIn(email, password) } },
                        onForgot = { message = null; screen = AuthScreen.FORGOT },
                        onSignUp = { message = null; screen = AuthScreen.SIGN_UP },
                        onBack = { message = null; screen = AuthScreen.WELCOME }
                    )
                    AuthScreen.SIGN_UP -> SignUpScreen(
                        busy = isBusy,
                        message = message,
                        messageIsError = messageIsError,
                        onSubmit = { displayName, username, email, password ->
                            runAuth { repository.signUp(displayName, username, email, password) }
                        },
                        onLogin = { message = null; screen = AuthScreen.LOGIN },
                        onBack = { message = null; screen = AuthScreen.WELCOME }
                    )
                    AuthScreen.FORGOT -> ForgotPasswordScreen(
                        busy = isBusy,
                        message = message,
                        messageIsError = messageIsError,
                        onSubmit = ::sendReset,
                        onBack = { message = null; screen = AuthScreen.LOGIN }
                    )
                    AuthScreen.RESET_PASSWORD -> ResetPasswordScreen(
                        busy = isBusy,
                        message = message,
                        messageIsError = messageIsError,
                        onSubmit = ::saveNewPassword,
                        onBack = { message = null; screen = AuthScreen.LOGIN }
                    )
                    AuthScreen.HOME -> HomeScreen(
                        name = user?.displayName ?: "صديق الأنمي",
                        email = user?.email.orEmpty(),
                        onSignOut = {
                            scope.launch {
                                isBusy = true
                                repository.signOut()
                                user = null
                                message = null
                                screen = AuthScreen.WELCOME
                                isBusy = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun BrandLogo(size: Int = 174) {
    Image(
        painter = painterResource(R.drawable.anime_black_logo),
        contentDescription = "شعار Anime Black",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, Color(0x663D9EFF)), CircleShape)
    )
}

@Composable
private fun WelcomeScreen(onSignUp: () -> Unit, onLogin: () -> Unit) {
    PageFrame {
        Spacer(Modifier.height(24.dp))
        BrandLogo(190)
        Spacer(Modifier.height(26.dp))
        Text("عالمك يبدأ هنا", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("مرحبًا بك في", color = MainText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Anime Black", color = Color(0xFF65BDFF), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "اكتشف، تابع، وشارك أجمل عوالم الأنمي في مساحة صُممت لأجلك.",
            color = MutedText,
            fontSize = 15.sp,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) { Text("إنشاء حساب جديد", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanelLight, contentColor = MainText)
        ) { Text("لدي حساب بالفعل — تسجيل الدخول", fontSize = 15.sp) }
        Spacer(Modifier.height(26.dp))
        Text("توصيات شخصية  ·  مجتمع آمن  ·  تجربة ممتعة", color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String, String) -> Unit,
    onForgot: () -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    PageFrame {
        PageTopBar(title = "Anime Black", onBack = onBack)
        Spacer(Modifier.height(28.dp))
        Text("أهلًا بعودتك ✦", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        Text("سجّل دخولك لمتابعة رحلتك في عالم الأنمي.", color = MutedText, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(27.dp))
        AuthInput(
            value = email,
            onValueChange = { email = it },
            label = "البريد الإلكتروني",
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email,
            isLtr = true
        )
        Spacer(Modifier.height(14.dp))
        PasswordInput(
            value = password,
            onValueChange = { password = it },
            label = "كلمة المرور",
            visible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text("تذكرني", color = MutedText, fontSize = 13.sp)
            }
            TextButton(onClick = onForgot) { Text("نسيت كلمة المرور؟", color = Color(0xFF75BFFF), fontSize = 13.sp) }
        }
        if (message != null) {
            Spacer(Modifier.height(7.dp))
            InlineMessage(message, messageIsError)
        }
        Spacer(Modifier.height(17.dp))
        PrimaryAction("تسجيل الدخول", busy, onClick = {
            when {
                email.isBlank() -> Unit
                password.isBlank() -> Unit
                else -> onSubmit(email, password)
            }
        })
        if (email.isBlank() || password.isBlank()) {
            Text("أدخل بريدك الإلكتروني وكلمة المرور للمتابعة.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
        }
        Spacer(Modifier.height(21.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ليس لديك حساب؟", color = MutedText, fontSize = 14.sp)
            TextButton(onClick = onSignUp) { Text("أنشئ حسابًا الآن", color = Cyan, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SignUpScreen(
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String, String, String, String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    PageFrame {
        PageTopBar(title = "حساب جديد", onBack = onBack)
        Spacer(Modifier.height(22.dp))
        Text("انضم إلى المجتمع", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        Text("أنشئ هويتك الخاصة وابدأ تجربتك المميزة.", color = MutedText, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(21.dp))
        AuthInput(displayName, { displayName = it }, "الاسم الظاهر", "كيف نناديك؟")
        Spacer(Modifier.height(12.dp))
        AuthInput(username, { username = it }, "اسم المستخدم", "مثال: kaito_sama", isLtr = true)
        Spacer(Modifier.height(12.dp))
        AuthInput(email, { email = it }, "البريد الإلكتروني", "name@example.com", KeyboardType.Email, isLtr = true)
        Spacer(Modifier.height(12.dp))
        PasswordInput(password, { password = it }, "كلمة المرور", passwordVisible, { passwordVisible = !passwordVisible })
        Spacer(Modifier.height(12.dp))
        PasswordInput(confirmPassword, { confirmPassword = it }, "تأكيد كلمة المرور", passwordVisible, { passwordVisible = !passwordVisible })
        Spacer(Modifier.height(8.dp))
        Text("استخدم 8 أحرف على الأقل لكلمة مرور أقوى.", color = MutedText, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
            Text("أوافق على شروط الاستخدام وسياسة الخصوصية.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
        }
        val currentMessage = localError ?: message
        if (currentMessage != null) {
            Spacer(Modifier.height(5.dp))
            InlineMessage(currentMessage, localError != null || messageIsError)
        }
        Spacer(Modifier.height(10.dp))
        PrimaryAction("إنشاء الحساب", busy, onClick = {
            localError = when {
                displayName.isBlank() -> "أدخل الاسم الظاهر."
                username.trim().removePrefix("@").length < 3 -> "اسم المستخدم يجب أن يكون 3 أحرف على الأقل."
                !email.contains('@') -> "أدخل بريدًا إلكترونيًا صحيحًا."
                password.length < 8 -> "استخدم 8 أحرف على الأقل لكلمة المرور."
                password != confirmPassword -> "كلمتا المرور غير متطابقتين."
                !acceptedTerms -> "وافق على الشروط وسياسة الخصوصية للمتابعة."
                else -> null
            }
            if (localError == null) onSubmit(displayName, username, email, password)
        })
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("لديك حساب؟", color = MutedText, fontSize = 14.sp)
            TextButton(onClick = onLogin) { Text("تسجيل الدخول", color = Cyan, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ForgotPasswordScreen(
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    PageFrame {
        PageTopBar(title = "استعادة الحساب", onBack = onBack)
        Spacer(Modifier.height(36.dp))
        Text("نسيت كلمة المرور؟", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Text("لا تقلق. أدخل بريدك الإلكتروني وسنرسل لك رابطًا آمنًا لإعادة تعيينها.", color = MutedText, fontSize = 14.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(25.dp))
        AuthInput(email, { email = it; localError = null }, "البريد الإلكتروني", "name@example.com", KeyboardType.Email, isLtr = true)
        val currentMessage = localError ?: message
        if (currentMessage != null) {
            Spacer(Modifier.height(13.dp))
            InlineMessage(currentMessage, localError != null || messageIsError)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryAction("إرسال رابط الاستعادة", busy, onClick = {
            localError = if (!email.contains('@')) "أدخل بريدًا إلكترونيًا صحيحًا." else null
            if (localError == null) onSubmit(email)
        })
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("تذكرت كلمة المرور؟ تسجيل الدخول", color = Cyan) }
    }
}

@Composable
private fun ResetPasswordScreen(
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    PageFrame {
        PageTopBar(title = "حماية الحساب", onBack = onBack)
        Spacer(Modifier.height(34.dp))
        Text("أنشئ كلمة مرور جديدة", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Text("اختر كلمة مرور قوية لم تستخدمها من قبل.", color = MutedText, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        PasswordInput(password, { password = it }, "كلمة المرور الجديدة", visible, { visible = !visible })
        Spacer(Modifier.height(13.dp))
        PasswordInput(confirmation, { confirmation = it }, "تأكيد كلمة المرور", visible, { visible = !visible })
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            InlineMessage(message, messageIsError)
        }
        Spacer(Modifier.height(19.dp))
        PrimaryAction("حفظ كلمة المرور", busy, onClick = { onSubmit(password, confirmation) })
    }
}

@Composable
private fun HomeScreen(name: String, email: String, onSignOut: () -> Unit) {
    PageFrame {
        Spacer(Modifier.height(20.dp))
        BrandLogo(142)
        Spacer(Modifier.height(24.dp))
        Text("أهلًا بك يا $name!", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Text("تم تسجيل الدخول بنجاح إلى Anime Black.", color = MutedText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
        if (email.isNotBlank()) {
            Spacer(Modifier.height(15.dp))
            Text(email, color = Color(0xFFB7D7FF), fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
        Surface(
            color = Panel,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0x332D8CFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "حسابك متصل بـSupabase. الخطوة التالية: إكمال الملف الشخصي والصفحة الرئيسية.",
                color = MutedText,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(18.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanelLight, contentColor = MainText)
        ) { Text("تسجيل الخروج") }
    }
}

@Composable
private fun PageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) { Text("→ رجوع", color = Color(0xFF75BFFF), fontSize = 14.sp) }
        Text(title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isLtr: Boolean = false
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides if (isLtr) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = MutedText) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            colors = fieldColors()
        )
    }
}

@Composable
private fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(15.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = onToggleVisibility) {
                Text(if (visible) "إخفاء" else "إظهار", color = Color(0xFF75BFFF), fontSize = 12.sp)
            }
        },
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MainText,
    unfocusedTextColor = MainText,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = Color(0xFF344052),
    focusedLabelColor = Color(0xFF75BFFF),
    unfocusedLabelColor = MutedText,
    cursorColor = Cyan,
    focusedContainerColor = Panel,
    unfocusedContainerColor = Panel
)

@Composable
private fun PrimaryAction(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = Color(0xFF315A8A))
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("لحظة من فضلك…")
        } else {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InlineMessage(text: String, isError: Boolean) {
    val color = if (isError) ErrorRed else SuccessGreen
    Surface(
        color = color.copy(alpha = 0.09f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, color = color, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(13.dp))
    }
}
