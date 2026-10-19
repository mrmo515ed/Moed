package com.animeblack.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.animeblack.app.auth.AnimeGenre
import com.animeblack.app.auth.AuthUser
import com.animeblack.app.auth.SupabaseAuthRepository
import kotlinx.coroutines.delay

private val Background = Color(0xFF090B12)
private val SurfaceColor = Color(0xFF121722)
private val SurfaceLight = Color(0xFF171E2D)
private val Blue = Color(0xFF2D8CFF)
private val Cyan = Color(0xFF23D8FF)
private val MainText = Color(0xFFF6F8FF)
private val Muted = Color(0xFF98A3BA)
private val Error = Color(0xFFFF718A)
private val Success = Color(0xFF4FE0A9)

@Composable
private fun PageFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 17.dp)
            .widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun BrandLogo(size: Int = 170) {
    Image(
        painter = painterResource(R.drawable.anime_black_logo),
        contentDescription = "شعار Anime Black",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(1.dp, Color(0x663D9EFF), CircleShape)
    )
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) { Text("→ رجوع", color = Color(0xFF75BFFF), fontSize = 14.sp) }
        Text(title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WelcomeScreen(
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
    onMethods: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit
) {
    PageFrame {
        Spacer(Modifier.height(12.dp))
        BrandLogo(186)
        Spacer(Modifier.height(23.dp))
        Text("عالمك يبدأ هنا", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Text("مرحبًا بك في", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Anime Black", color = Color(0xFF65BDFF), fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "اكتشف، تابع، وشارك أجمل عوالم الأنمي في مساحة صُممت لأجلك.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 23.sp
        )
        Spacer(Modifier.height(25.dp))
        PrimaryButton("إنشاء حساب جديد", false, onSignUp)
        Spacer(Modifier.height(11.dp))
        SecondaryButton("لدي حساب بالفعل — تسجيل الدخول", onLogin)
        TextButton(onClick = onMethods) { Text("خيارات تسجيل الدخول", color = Cyan) }
        Spacer(Modifier.height(10.dp))
        Text("توصيات شخصية  ·  مجتمع آمن  ·  تجربة ممتعة", color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onTerms) { Text("الشروط", color = Muted, fontSize = 12.sp) }
            Text("·", color = Muted)
            TextButton(onClick = onPrivacy) { Text("الخصوصية", color = Muted, fontSize = 12.sp) }
        }
    }
}

@Composable
fun MethodsScreen(
    phoneAvailable: Boolean,
    googleAvailable: Boolean,
    onEmail: () -> Unit,
    onPhone: () -> Unit,
    onGoogle: () -> Unit,
    onBack: () -> Unit
) {
    PageFrame {
        TopBar("دخول آمن", onBack)
        Spacer(Modifier.height(18.dp))
        Text("اختر طريقة الدخول", color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("استخدم الطريقة الأنسب للوصول إلى حسابك.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
        Spacer(Modifier.height(23.dp))
        MethodCard("✉", "البريد الإلكتروني", "الدخول بكلمة المرور", "متاح", true, onEmail)
        Spacer(Modifier.height(11.dp))
        MethodCard("⌕", "رقم الهاتف", "تحقق برمز SMS لمرة واحدة", if (phoneAvailable) "متاح" else "يتطلب إعداد SMS", phoneAvailable, onPhone)
        Spacer(Modifier.height(11.dp))
        MethodCard("G", "Google", "دخول آمن بنقرة واحدة", if (googleAvailable) "متاح" else "يتطلب إعداد OAuth", googleAvailable, onGoogle)
        Spacer(Modifier.height(18.dp))
        InlineNote("طرق الهاتف وGoogle تظهر هنا، لكن تبقى غير مفعّلة حتى يكتمل إعداد مزوّد SMS وبيانات OAuth في Supabase.", true)
    }
}

@Composable
private fun MethodCard(icon: String, title: String, subtitle: String, status: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        color = SurfaceColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (enabled) Color(0x332D8CFF) else Color(0x22334455))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Blue.copy(alpha = 0.13f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(icon, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(title, color = MainText, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text(status, color = if (enabled) Success else Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun LoginScreen(
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String, String, Boolean) -> Unit,
    onForgot: () -> Unit,
    onSignUp: () -> Unit,
    onMethods: () -> Unit,
    onBack: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }

    PageFrame {
        TopBar("Anime Black", onBack)
        Spacer(Modifier.height(20.dp))
        Text("أهلًا بعودتك ✦", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("سجّل دخولك لمتابعة رحلتك في عالم الأنمي.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
        Spacer(Modifier.height(23.dp))
        AuthField(email, { email = it; validation = null }, "البريد الإلكتروني", "name@example.com", KeyboardType.Email, ltr = true)
        Spacer(Modifier.height(13.dp))
        PasswordField(password, { password = it; validation = null }, "كلمة المرور", visible, { visible = !visible })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text("تذكرني", color = Muted, fontSize = 13.sp)
            }
            TextButton(onClick = onForgot) { Text("نسيت كلمة المرور؟", color = Color(0xFF75BFFF), fontSize = 13.sp) }
        }
        if (validation != null) { Spacer(Modifier.height(5.dp)); InlineNote(validation!!, true) }
        if (message != null) { Spacer(Modifier.height(7.dp)); InlineNote(message, messageIsError) }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("تسجيل الدخول", busy) {
            validation = when {
                !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) -> "أدخل بريدًا إلكترونيًا صحيحًا."
                password.isBlank() -> "أدخل كلمة المرور."
                else -> null
            }
            if (validation == null) onSubmit(email, password, rememberMe)
        }
        Spacer(Modifier.height(9.dp))
        SecondaryButton("خيارات الدخول الأخرى", onMethods)
        Spacer(Modifier.height(17.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ليس لديك حساب؟", color = Muted, fontSize = 14.sp)
            TextButton(onClick = onSignUp) { Text("أنشئ حسابًا الآن", color = Cyan, fontWeight = FontWeight.Bold) }
        }
    }
}

private enum class UsernameState { IDLE, CHECKING, AVAILABLE, TAKEN }

@Composable
fun SignUpScreen(
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String, String, String, String) -> Unit,
    onCheckUsername: suspend (String) -> Boolean,
    onLogin: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onBack: () -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var usernameState by remember { mutableStateOf(UsernameState.IDLE) }
    val normalizedUsername = username.trim().removePrefix("@").lowercase()

    LaunchedEffect(normalizedUsername) {
        usernameState = UsernameState.IDLE
        if (normalizedUsername.length >= 3 && normalizedUsername.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            delay(500)
            usernameState = UsernameState.CHECKING
            usernameState = runCatching { if (onCheckUsername(normalizedUsername)) UsernameState.AVAILABLE else UsernameState.TAKEN }
                .getOrDefault(UsernameState.IDLE)
        }
    }

    PageFrame {
        TopBar("حساب جديد", onBack)
        Spacer(Modifier.height(13.dp))
        Text("انضم إلى المجتمع", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("أنشئ هويتك الخاصة وابدأ تجربتك المميزة.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(19.dp))
        AuthField(displayName, { displayName = it; localError = null }, "الاسم الظاهر", "كيف نناديك؟")
        Spacer(Modifier.height(11.dp))
        AuthField(username, { username = it; localError = null }, "اسم المستخدم", "3–20 حرفًا: a-z، 0-9، _", ltr = true)
        when (usernameState) {
            UsernameState.CHECKING -> Text("نتحقق من توفر الاسم…", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
            UsernameState.AVAILABLE -> Text("✓ اسم المستخدم متاح", color = Success, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
            UsernameState.TAKEN -> Text("هذا الاسم مستخدم. جرّب اسمًا آخر.", color = Error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
            UsernameState.IDLE -> if (username.isNotBlank() && !normalizedUsername.matches(Regex("^[a-z0-9_]{3,20}$"))) {
                Text("استخدم 3–20 حرفًا إنجليزيًا صغيرًا أو رقمًا أو _.", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
            }
        }
        Spacer(Modifier.height(11.dp))
        AuthField(email, { email = it; localError = null }, "البريد الإلكتروني", "name@example.com", KeyboardType.Email, ltr = true)
        Spacer(Modifier.height(11.dp))
        PasswordField(password, { password = it; localError = null }, "كلمة المرور", visible, { visible = !visible })
        PasswordStrength(password)
        Spacer(Modifier.height(10.dp))
        PasswordField(confirm, { confirm = it; localError = null }, "تأكيد كلمة المرور", visible, { visible = !visible })
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Text("أوافق على", color = Muted, fontSize = 12.sp)
            TextButton(onClick = onTerms) { Text("الشروط", color = Color(0xFF75BFFF), fontSize = 12.sp) }
            Text("و", color = Muted, fontSize = 12.sp)
            TextButton(onClick = onPrivacy) { Text("الخصوصية", color = Color(0xFF75BFFF), fontSize = 12.sp) }
        }
        val visibleMessage = localError ?: message
        if (visibleMessage != null) { Spacer(Modifier.height(5.dp)); InlineNote(visibleMessage, localError != null || messageIsError) }
        Spacer(Modifier.height(10.dp))
        PrimaryButton("إنشاء الحساب", busy) {
            localError = when {
                displayName.trim().length < 2 -> "اكتب اسمًا ظاهرًا من حرفين على الأقل."
                !normalizedUsername.matches(Regex("^[a-z0-9_]{3,20}$")) -> "اسم المستخدم غير صالح. استخدم 3–20 حرفًا ورقمًا أو _."
                usernameState == UsernameState.TAKEN -> "اسم المستخدم مستخدم من قبل."
                !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) -> "أدخل بريدًا إلكترونيًا صحيحًا."
                password.length < 8 -> "استخدم 8 أحرف على الأقل لكلمة المرور."
                password.none(Char::isLetter) || password.none(Char::isDigit) -> "اجمع بين حرف واحد ورقم واحد على الأقل."
                password != confirm -> "كلمتا المرور غير متطابقتين."
                !accepted -> "وافق على الشروط وسياسة الخصوصية للمتابعة."
                usernameState == UsernameState.CHECKING -> "انتظر حتى ننتهي من التحقق من اسم المستخدم."
                else -> null
            }
            if (localError == null) onSubmit(displayName, normalizedUsername, email.trim(), password)
        }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("لديك حساب؟", color = Muted, fontSize = 14.sp)
            TextButton(onClick = onLogin) { Text("تسجيل الدخول", color = Cyan, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PasswordStrength(password: String) {
    val score = when {
        password.length >= 12 && password.any(Char::isUpperCase) && password.any(Char::isDigit) && password.any { !it.isLetterOrDigit() } -> 4
        password.length >= 10 && password.any(Char::isLetter) && password.any(Char::isDigit) -> 3
        password.length >= 8 && password.any(Char::isLetter) && password.any(Char::isDigit) -> 2
        password.length >= 8 -> 1
        else -> 0
    }
    val color = when (score) { 0, 1 -> Error; 2 -> Color(0xFFFFC45B); else -> Success }
    val title = when (score) { 0 -> "استخدم 8 أحرف على الأقل"; 1 -> "ضعيفة"; 2 -> "متوسطة"; 3 -> "جيدة"; else -> "قوية" }
    Column(Modifier.fillMaxWidth().padding(top = 7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (index < score) color else Color(0xFF303848)))
            }
        }
        Text("قوة كلمة المرور: $title", color = if (score == 0) Muted else color, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun VerifyEmailScreen(
    email: String,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onResend: () -> Unit,
    onOpenMail: () -> Unit,
    onChangeEmail: () -> Unit,
    onBack: () -> Unit
) {
    var seconds by rememberSaveable(email) { mutableStateOf(40) }
    var resendCount by rememberSaveable(email) { mutableStateOf(0) }
    LaunchedEffect(email, resendCount) {
        seconds = 40
        while (seconds > 0) { delay(1_000); seconds-- }
    }
    PageFrame {
        TopBar("تحقق الحساب", onBack)
        Spacer(Modifier.height(25.dp))
        StateIcon("✉")
        Spacer(Modifier.height(20.dp))
        Text("أكد بريدك الإلكتروني", color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("أرسلنا رابط تحقق إلى هذا البريد. افتحه على هذا الجهاز للعودة إلى التطبيق تلقائيًا.", color = Muted, fontSize = 14.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
        Spacer(Modifier.height(17.dp))
        InlineNote(email.ifBlank { "البريد الذي سجلت به" }, false, ltr = true)
        Spacer(Modifier.height(17.dp))
        if (message != null) InlineNote(message, messageIsError)
        PrimaryButton("فتح تطبيق البريد", false, onOpenMail)
        Spacer(Modifier.height(11.dp))
        PrimaryButton(
            if (seconds > 0) "إعادة إرسال الرسالة بعد ${seconds}s" else "إعادة إرسال رسالة التحقق",
            busy,
            onClick = { onResend(); resendCount++ },
            enabled = seconds == 0
        )
        TextButton(onClick = onChangeEmail) { Text("أدخلت بريدًا خاطئًا؟ تغييره", color = Color(0xFF75BFFF)) }
        Text("إذا لم تظهر الرسالة، افحص مجلد الرسائل غير المرغوب فيها.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
fun EmailSentScreen(
    email: String,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onBack: () -> Unit
) {
    var seconds by rememberSaveable(email) { mutableStateOf(40) }
    var resendCount by rememberSaveable(email) { mutableStateOf(0) }
    LaunchedEffect(email, resendCount) {
        seconds = if (resendCount == 0) 0 else 40
        while (seconds > 0) { delay(1_000); seconds-- }
    }
    PageFrame {
        TopBar("استعادة الحساب", onBack)
        Spacer(Modifier.height(26.dp))
        StateIcon("✓")
        Spacer(Modifier.height(18.dp))
        Text("تحقق من بريدك", color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("إذا كان البريد مرتبطًا بحساب، أرسلنا إليه رابطًا آمنًا لإعادة تعيين كلمة المرور.", color = Muted, fontSize = 14.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(17.dp))
        InlineNote(email, false, ltr = true)
        Spacer(Modifier.height(14.dp))
        InlineNote("قد يستغرق وصول الرسالة بضع دقائق. افحص مجلد الرسائل غير المرغوب فيها أيضًا.", false)
        if (message != null) { Spacer(Modifier.height(11.dp)); InlineNote(message, messageIsError) }
        Spacer(Modifier.height(17.dp))
        PrimaryButton("فتحت الرابط — متابعة", false, onBack)
        Spacer(Modifier.height(8.dp))
        TextButton(enabled = !busy && seconds == 0, onClick = { onResend(); resendCount++ }) {
            Text(if (seconds > 0) "إعادة إرسال الرابط بعد ${seconds}s" else "إعادة إرسال الرابط", color = if (seconds == 0) Cyan else Muted)
        }
        TextButton(onClick = onChangeEmail) { Text("هل أدخلت بريدًا خاطئًا؟ تغييره", color = Color(0xFF75BFFF)) }
    }
}

@Composable
fun ForgotPasswordScreen(busy: Boolean, message: String?, messageIsError: Boolean, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    PageFrame {
        TopBar("استعادة الحساب", onBack)
        Spacer(Modifier.height(29.dp))
        Text("نسيت كلمة المرور؟", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("لا تقلق. أدخل بريدك الإلكتروني وسنرسل لك رابطًا آمنًا لإعادة تعيينها.", color = Muted, fontSize = 14.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
        Spacer(Modifier.height(24.dp))
        AuthField(email, { email = it; validation = null }, "البريد الإلكتروني", "name@example.com", KeyboardType.Email, ltr = true)
        val shown = validation ?: message
        if (shown != null) { Spacer(Modifier.height(12.dp)); InlineNote(shown, validation != null || messageIsError) }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("إرسال رابط الاستعادة", busy) {
            validation = if (email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) null else "أدخل بريدًا إلكترونيًا صحيحًا."
            if (validation == null) onSubmit(email.trim())
        }
        TextButton(onClick = onBack) { Text("تذكرت كلمة المرور؟ تسجيل الدخول", color = Cyan) }
    }
}

@Composable
fun PhoneScreen(
    available: Boolean,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSend: (String, Boolean, String, String) -> Unit,
    onBack: () -> Unit
) {
    var countryCode by rememberSaveable { mutableStateOf("+967") }
    var phoneDigits by rememberSaveable { mutableStateOf("") }
    var isSignup by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var countryMenu by remember { mutableStateOf(false) }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    PageFrame {
        TopBar("رقم الهاتف", onBack)
        Spacer(Modifier.height(23.dp))
        Text(if (isSignup) "إنشاء حساب بالهاتف" else "الدخول برقم الهاتف", color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("سنرسل رمزًا لمرة واحدة للتحقق من رقمك.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !isSignup, onClick = { isSignup = false })
            Text("لدي حساب", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.width(13.dp))
            RadioButton(selected = isSignup, onClick = { isSignup = true })
            Text("حساب جديد", color = Muted, fontSize = 13.sp)
        }
        if (isSignup) {
            AuthField(displayName, { displayName = it }, "الاسم الظاهر", "كيف نناديك؟")
            Spacer(Modifier.height(10.dp))
            AuthField(username, { username = it }, "اسم المستخدم", "3–20 حرفًا", ltr = true)
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box {
                SecondaryButton(countryCode, { countryMenu = true }, Modifier.width(105.dp))
                DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                    listOf("+967" to "اليمن", "+966" to "السعودية", "+20" to "مصر", "+971" to "الإمارات").forEach { (code, country) ->
                        DropdownMenuItem(text = { Text("$country  $code") }, onClick = { countryCode = code; countryMenu = false })
                    }
                }
            }
            Spacer(Modifier.width(9.dp))
            AuthField(phoneDigits, { phoneDigits = it.filter(Char::isDigit).take(12); validation = null }, "رقم الهاتف", "77 000 0000", KeyboardType.Phone, ltr = true, modifier = Modifier.weight(1f))
        }
        Text("رقمك يستخدم للتحقق فقط، ولن يظهر في ملفك العام.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        if (!available) { Spacer(Modifier.height(13.dp)); InlineNote("تسجيل الهاتف غير مفعّل في مشروع Supabase بعد. يلزم تفعيل مزوّد SMS وإضافة بياناته قبل إرسال رموز حقيقية.", true) }
        val shown = validation ?: message
        if (shown != null) { Spacer(Modifier.height(11.dp)); InlineNote(shown, validation != null || messageIsError) }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("إرسال رمز التحقق", busy, onClick = {
            validation = when {
                phoneDigits.length < 7 -> "أدخل رقم هاتف صحيحًا."
                isSignup && displayName.trim().length < 2 -> "أدخل الاسم الظاهر."
                isSignup && username.trim().length < 3 -> "أدخل اسم مستخدم من 3 أحرف على الأقل."
                else -> null
            }
            if (validation == null) onSend(countryCode + phoneDigits, isSignup, displayName, username)
        }, enabled = available)
    }
}

@Composable
fun OtpScreen(
    phone: String,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onEditPhone: () -> Unit,
    onBack: () -> Unit
) {
    var code by rememberSaveable(phone) { mutableStateOf("") }
    var seconds by rememberSaveable(phone) { mutableStateOf(60) }
    LaunchedEffect(phone) { seconds = 60; while (seconds > 0) { delay(1_000); seconds-- } }
    PageFrame {
        TopBar("تحقق الهاتف", onBack)
        Spacer(Modifier.height(22.dp))
        StateIcon("⌕")
        Spacer(Modifier.height(17.dp))
        Text("أدخل رمز التحقق", color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("أرسلنا رمزًا من 6 أرقام إلى الرقم التالي.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(13.dp))
        InlineNote(phone, false, ltr = true)
        Spacer(Modifier.height(18.dp))
        AuthField(code, { code = it.filter(Char::isDigit).take(6) }, "رمز التحقق", "000000", KeyboardType.NumberPassword, ltr = true)
        Text(if (seconds > 0) "ينتهي الرمز خلال 00:${seconds.toString().padStart(2, '0')}" else "انتهت صلاحية الرمز أو يمكنك طلب رمز جديد.", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        if (message != null) { Spacer(Modifier.height(10.dp)); InlineNote(message, messageIsError) }
        Spacer(Modifier.height(17.dp))
        PrimaryButton("تأكيد الرمز", busy) { onVerify(code) }
        TextButton(enabled = !busy && seconds == 0, onClick = onResend) { Text("إعادة إرسال الرمز", color = if (seconds == 0) Cyan else Muted) }
        TextButton(onClick = onEditPhone) { Text("تعديل رقم الهاتف", color = Color(0xFF75BFFF)) }
    }
}

@Composable
fun ResetPasswordScreen(busy: Boolean, message: String?, messageIsError: Boolean, onSubmit: (String, String) -> Unit, onBack: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    PageFrame {
        TopBar("حماية الحساب", onBack)
        Spacer(Modifier.height(28.dp))
        Text("أنشئ كلمة مرور جديدة", color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("اختر كلمة مرور قوية لم تستخدمها من قبل.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(23.dp))
        PasswordField(password, { password = it; localError = null }, "كلمة المرور الجديدة", visible, { visible = !visible })
        PasswordStrength(password)
        Spacer(Modifier.height(12.dp))
        PasswordField(confirmation, { confirmation = it; localError = null }, "تأكيد كلمة المرور", visible, { visible = !visible })
        val shown = localError ?: message
        if (shown != null) { Spacer(Modifier.height(12.dp)); InlineNote(shown, localError != null || messageIsError) }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("حفظ كلمة المرور", busy) {
            localError = when {
                password.length < 8 -> "استخدم 8 أحرف على الأقل."
                password.none(Char::isLetter) || password.none(Char::isDigit) -> "اجمع بين حرف ورقم على الأقل."
                confirmation != password -> "كلمتا المرور غير متطابقتين."
                else -> null
            }
            if (localError == null) onSubmit(password, confirmation)
        }
    }
}

@Composable
fun ProfileScreen(
    user: AuthUser?,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSubmit: (String, String, String) -> Unit,
    onAvatarSelected: (android.net.Uri) -> Unit,
    onBack: () -> Unit
) {
    var displayName by rememberSaveable(user?.id) { mutableStateOf(user?.displayName.orEmpty()) }
    var username by rememberSaveable(user?.id) { mutableStateOf(user?.username.orEmpty()) }
    var bio by rememberSaveable(user?.id) { mutableStateOf(user?.bio.orEmpty()) }
    var accepted by rememberSaveable { mutableStateOf(true) }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onAvatarSelected) }
    PageFrame {
        TopBar("إعداد الحساب", onBack)
        Spacer(Modifier.height(8.dp))
        Text("عرّفنا بك", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("أكمل ملفك الشخصي ليعرفك مجتمع Anime Black.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(color = SurfaceLight, shape = CircleShape, modifier = Modifier.size(94.dp), border = BorderStroke(1.dp, Color(0x553D9EFF))) {
                Box(contentAlignment = Alignment.Center) { Text(displayName.firstOrNull()?.uppercase() ?: "♙", color = Cyan, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
            }
            TextButton(onClick = { imagePicker.launch("image/*") }) { Text("＋", color = Color.White, fontSize = 18.sp) }
        }
        Text("صورة شخصية اختيارية · JPG/PNG", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        AuthField(displayName, { displayName = it; validation = null }, "الاسم الظاهر", "كيف نناديك؟")
        Spacer(Modifier.height(12.dp))
        AuthField(username, { username = it; validation = null }, "اسم المستخدم", "3–20 حرفًا: a-z، 0-9، _", ltr = true)
        Spacer(Modifier.height(12.dp))
        BioField(bio, { bio = it.take(180) })
        Text("${bio.length}/180", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).padding(top = 5.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Text("أوافق على ظهور الاسم والنبذة ضمن ملفي العام.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        val shown = validation ?: message
        if (shown != null) { Spacer(Modifier.height(7.dp)); InlineNote(shown, validation != null || messageIsError) }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("حفظ ومتابعة", busy) {
            validation = when {
                user == null -> "سجّل الدخول مجددًا للمتابعة."
                displayName.trim().length < 2 -> "أدخل اسمًا ظاهرًا من حرفين على الأقل."
                !username.trim().removePrefix("@").lowercase().matches(Regex("^[a-z0-9_]{3,20}$")) -> "اسم المستخدم يجب أن يكون 3–20 حرفًا أو رقمًا أو _."
                !accepted -> "أكد موافقتك على ظهور بيانات الملف العام."
                else -> null
            }
            if (validation == null) onSubmit(displayName, username.trim().removePrefix("@").lowercase(), bio)
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun BioField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("نبذة تعريفية (اختياري)") },
        placeholder = { Text("اكتب سطرًا قصيرًا عن أنمياتك المفضلة…", color = Muted) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 4,
        shape = RoundedCornerShape(15.dp),
        colors = fieldColors()
    )
}

@Composable
fun InterestsScreen(
    repository: SupabaseAuthRepository,
    userId: String?,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSave: (Set<String>) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    var genres by remember { mutableStateOf<List<AnimeGenre>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(userId) {
        loading = true
        loadError = null
        try {
            genres = repository.loadGenres()
            if (userId != null) selected = repository.loadMyInterests(userId)
        } catch (_: Exception) {
            loadError = "تعذّر تحميل التفضيلات. تحقق من اتصالك ثم أعد المحاولة."
        } finally { loading = false }
    }
    PageFrame {
        TopBar("تفضيلاتك", onBack)
        Spacer(Modifier.height(8.dp))
        Text("ما الذي تحب مشاهدته؟", color = MainText, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("اختر 3 اهتمامات على الأقل لنقدم لك توصيات مناسبة.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
        Spacer(Modifier.height(14.dp))
        Text("تم اختيار ${selected.size} / 10", color = if (selected.size >= 3) Cyan else Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(11.dp))
        if (loading) CircularProgressIndicator(color = Cyan, modifier = Modifier.size(24.dp))
        if (loadError != null) InlineNote(loadError!!, true)
        genres.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                rowItems.forEach { genre ->
                    val isSelected = genre.id in selected
                    Surface(
                        modifier = Modifier.weight(1f).clickable {
                            selected = if (isSelected) selected - genre.id else if (selected.size < 10) selected + genre.id else selected
                        },
                        color = if (isSelected) Color(0xFF142D4F) else SurfaceColor,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, if (isSelected) Blue else Color(0xFF293448))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (isSelected) "✓ ${genre.nameArabic}" else genre.nameArabic, color = if (isSelected) Cyan else MainText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(genre.nameEnglish, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (message != null) { Spacer(Modifier.height(8.dp)); InlineNote(message, messageIsError) }
        if (selected.size in 1..2) { Spacer(Modifier.height(5.dp)); InlineNote("أضف ${3 - selected.size} اهتمامات أخرى للمتابعة، أو تخطَّ هذه الخطوة.", false) }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "حفظ التفضيلات",
            busy,
            onClick = { onSave(selected) },
            enabled = !loading && loadError == null && selected.size >= 3
        )
        TextButton(onClick = onSkip, enabled = !busy) { Text("تخطي الآن", color = Muted) }
    }
}

@Composable
fun SuccessScreen(name: String, onContinue: () -> Unit) {
    PageFrame {
        Spacer(Modifier.height(25.dp))
        StateIcon("✓", success = true)
        Spacer(Modifier.height(20.dp))
        Text("أهلًا بك يا $name!", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("تم إعداد حسابك وحفظ تفضيلاتك بنجاح. عالم الأنمي بانتظارك.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(22.dp))
        InlineNote("✓  تم تأكيد بريدك وحماية حسابك\n✓  تم حفظ ملفك الشخصي\n✓  توصياتك أصبحت جاهزة", false)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("استكشف Anime Black", false, onContinue)
    }
}

@Composable
fun LegalScreen(title: String, onBack: () -> Unit, isPrivacy: Boolean) {
    PageFrame {
        TopBar("مركز السياسات", onBack)
        Spacer(Modifier.height(13.dp))
        Text(title, color = MainText, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("آخر تحديث: 19 أكتوبر 2026", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 17.dp))
        if (isPrivacy) {
            LegalSection("1. المعلومات التي نجمعها", "نخزن البريد الإلكتروني وبيانات الملف الشخصي والاهتمامات اللازمة لتشغيل حسابك. لا نطلب كلمة مرورك من أي جهة خارج Supabase Auth.")
            LegalSection("2. استخدام المعلومات", "نستخدم بياناتك لتسجيل الدخول، حماية الحساب، عرض ملفك، وتخصيص توصيات الأنمي. لا نبيع بياناتك الشخصية.")
            LegalSection("3. التخزين والأمان", "تُحفظ بيانات الحساب في قاعدة بيانات Supabase مع سياسات وصول. تُخزن جلسة التطبيق محليًا بتشفير Android Keystore.")
            LegalSection("4. خياراتك", "يمكنك تعديل ملفك واهتماماتك أو تسجيل الخروج من التطبيق. اطلب حذف الحساب أو البيانات عبر قنوات الدعم التي سيضيفها مالك التطبيق.")
            LegalSection("5. التغييرات", "قد نحدّث هذه السياسة عند إضافة ميزات جديدة. سنعرض تاريخ التحديث في هذه الصفحة.")
        } else {
            LegalSection("1. قبول الشروط", "باستخدام Anime Black، توافق على هذه الشروط وعلى الالتزام بقواعد مجتمع محبي الأنمي.")
            LegalSection("2. حسابك", "أنت مسؤول عن سرية بيانات الدخول وعن النشاط الذي يتم عبر حسابك. استخدم كلمة مرور قوية وأبلغ مالك التطبيق عن أي استخدام غير مصرح به.")
            LegalSection("3. سلوك المجتمع", "احترم الأعضاء، ولا تنشر محتوى مسيئًا أو ينتهك حقوق الآخرين أو القوانين المعمول بها.")
            LegalSection("4. المحتوى والملكية", "احتفظ بحقوق المحتوى الذي تنشره، واحصل على الأذونات اللازمة قبل نشر أعمال الآخرين.")
            LegalSection("5. التقييد والإنهاء", "قد تُقيّد الحسابات عند إساءة الاستخدام أو الإضرار بالمجتمع، مع إتاحة التواصل لمراجعة الحالة.")
            LegalSection("6. التغييرات", "قد تتغير الميزات والشروط مع تطور التطبيق. استمرارك باستخدامه بعد التحديث يعني قبول النسخة الجديدة.")
        }
        Spacer(Modifier.height(14.dp))
        SecondaryButton("فهمت", onBack)
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 15.dp)) {
        Text(title, color = Color(0xFFB8D9FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Muted, fontSize = 13.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun StatusScreen(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    PageFrame {
        Spacer(Modifier.height(24.dp))
        StateIcon(if (title.contains("اتصال")) "⌁" else "⊘", success = false)
        Spacer(Modifier.height(20.dp))
        Text(title, color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text(body, color = Muted, fontSize = 14.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
        Spacer(Modifier.height(22.dp))
        PrimaryButton(action, false, onAction)
        if (secondaryAction != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(secondaryAction, onSecondary)
        }
    }
}

@Composable
fun HomeScreen(user: AuthUser?, busy: Boolean, onSignOut: () -> Unit, onEditProfile: () -> Unit) {
    PageFrame {
        Spacer(Modifier.height(15.dp))
        BrandLogo(145)
        Spacer(Modifier.height(22.dp))
        Text("أهلًا بك يا ${user?.displayName ?: "صديق الأنمي"}!", color = MainText, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("تم تسجيل الدخول بنجاح إلى Anime Black.", color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        user?.email?.takeIf(String::isNotBlank)?.let { Text(it, color = Color(0xFFB7D7FF), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 13.dp)) }
        Spacer(Modifier.height(20.dp))
        InlineNote("المصادقة والجلسة والملف الشخصي متصلة بـSupabase. صفحة المحتوى الرئيسية ستكون الخطوة التالية بعد اعتماد تدفق الحساب.", false)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("تعديل الملف الشخصي", onEditProfile)
        Spacer(Modifier.height(9.dp))
        PrimaryButton("تسجيل الخروج", busy, onSignOut)
    }
}

@Composable
private fun StateIcon(symbol: String, success: Boolean = false) {
    Surface(
        color = if (success) Success.copy(alpha = 0.12f) else Blue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, if (success) Success.copy(alpha = 0.35f) else Blue.copy(alpha = 0.35f)),
        modifier = Modifier.size(76.dp)
    ) { Box(contentAlignment = Alignment.Center) { Text(symbol, color = if (success) Success else Cyan, fontSize = 32.sp, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    ltr: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = Muted) },
            modifier = modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = fieldColors()
        )
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(15.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { TextButton(onClick = onToggle) { Text(if (visible) "إخفاء" else "إظهار", color = Color(0xFF75BFFF), fontSize = 12.sp) } },
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MainText,
    unfocusedTextColor = MainText,
    focusedBorderColor = Blue,
    unfocusedBorderColor = Color(0xFF344052),
    focusedLabelColor = Color(0xFF75BFFF),
    unfocusedLabelColor = Muted,
    cursorColor = Cyan,
    focusedContainerColor = SurfaceColor,
    unfocusedContainerColor = SurfaceColor
)

@Composable
private fun PrimaryButton(label: String, busy: Boolean, onClick: () -> Unit) {
    PrimaryButton(label, busy, enabled = true, onClick = onClick)
}

@Composable
private fun PrimaryButton(label: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy && enabled,
        modifier = Modifier.fillMaxWidth().height(53.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, disabledContainerColor = Color(0xFF315A8A))
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(9.dp))
            Text("لحظة من فضلك…")
        } else Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(51.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = MainText, disabledContainerColor = Color(0xFF141923), disabledContentColor = Muted),
        border = BorderStroke(1.dp, Color(0x223D9EFF))
    ) { Text(label, fontSize = 14.sp) }
}

@Composable
private fun InlineNote(text: String, isError: Boolean, ltr: Boolean = false) {
    val color = if (isError) Error else Success
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl) {
        Surface(
            color = color.copy(alpha = 0.08f),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) { Text(text, color = color, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(12.dp)) }
    }
}
