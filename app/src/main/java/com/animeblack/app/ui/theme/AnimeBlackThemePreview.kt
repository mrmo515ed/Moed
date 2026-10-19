package com.animeblack.app.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design-system specimen only. Text here is fixture copy for typography/layout review,
 * not app data and never used as a data source by the product.
 */
@Preview(
    name = "Anime Black · Theme prototype · RTL",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF07090D,
    widthDp = 390,
    heightDp = 900
)
@Composable
fun AnimeBlackThemePreview() {
    AnimeBlackTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    BrandHeader()
                    PaletteSpecimen()
                    TypographySpecimen()
                    ControlsSpecimen()
                    SurfacesSpecimen()
                    StatusSpecimen()
                    Text(
                        "نموذج نظام التصميم — ليس شاشة منتج ولا بيانات حقيقية",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = AnimeBlackPalette.SurfaceRaised,
            border = BorderStroke(1.dp, AnimeBlackPalette.Primary.copy(alpha = .55f))
        ) {
            Image(
                painter = painterResource(com.animeblack.app.R.drawable.anime_black_logo),
                contentDescription = "شعار Anime Black",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f)) {
            Text("ANIME BLACK", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("نظام مرئي داكن · معاينة أولية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = CircleShape, color = AnimeBlackPalette.PrimaryContainer) {
            Text("DARK", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = AnimeBlackPalette.OnPrimaryContainer)
        }
    }
}

@Composable
private fun PaletteSpecimen() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("01 · الألوان / COLOR ROLES")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorSwatch("Brand blue", "#00A3FF", AnimeBlackPalette.Primary, Modifier.weight(1f))
            ColorSwatch("Violet", "#B69CFF", AnimeBlackPalette.Secondary, Modifier.weight(1f))
            ColorSwatch("Success", "#67D99A", AnimeBlackPalette.Success, Modifier.weight(1f))
            ColorSwatch("Warning", "#FFC36B", AnimeBlackPalette.Warning, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceSample("Background", "#07090D", AnimeBlackPalette.Background, Modifier.weight(1f))
            SurfaceSample("Surface", "#0D1117", AnimeBlackPalette.Surface, Modifier.weight(1f))
            SurfaceSample("Raised", "#141B24", AnimeBlackPalette.SurfaceRaised, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TypographySpecimen() {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SectionLabel("02 · الخطوط / TYPE")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Cairo · عناوين", style = MaterialTheme.typography.labelMedium, color = AnimeBlackPalette.Primary)
                    Text("أنمي بلا حدود", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("ANIME BLACK  ·  012345", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(
                Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Tajawal · نصوص", style = MaterialTheme.typography.labelMedium, color = AnimeBlackPalette.Secondary)
                    Text("مرحبًا بك في المجتمع", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("عربي 123 · English 456", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("عنوان رئيسي · وصف ثانوي · الأرقام ١٢٣ / 123", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ControlsSpecimen() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("03 · المكونات / CONTROLS")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {},
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = MaterialTheme.shapes.small
            ) { Text("إجراء أساسي", style = MaterialTheme.typography.labelLarge) }
            OutlinedButton(onClick = {}, modifier = Modifier.height(48.dp), shape = MaterialTheme.shapes.small) {
                Text("ثانوي", style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = true, onClick = {}, label = { Text("محدد") })
            FilterChip(selected = false, onClick = {}, label = { Text("غير محدد") })
            FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("معطل") })
        }
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("ابحث في التطبيق") },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Search, contentDescription = "بحث") },
            shape = MaterialTheme.shapes.small
        )
        TextButton(onClick = {}) { Text("زر نصي / إجراء أقل بروزًا") }
    }
}

@Composable
private fun SurfacesSpecimen() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("04 · الأسطح / SHAPES")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.size(46.dp), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("بطاقة مكوّن", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("حدود رفيعة · لون سطح ثابت · نصف قطر 24dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Icon(Icons.Outlined.Info, contentDescription = "معلومات", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StatusSpecimen() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("05 · حالات النظام / STATUS")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatusBadge("نجاح", Icons.Outlined.CheckCircle, AnimeBlackPalette.Success, Modifier.weight(1f))
            StatusBadge("تنبيه", Icons.Outlined.WarningAmber, AnimeBlackPalette.Warning, Modifier.weight(1f))
            StatusBadge("خطأ", Icons.Outlined.ErrorOutline, AnimeBlackPalette.Error, Modifier.weight(1f))
            StatusBadge("معلومة", Icons.Outlined.Info, AnimeBlackPalette.Info, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.35.sp)
}

@Composable
private fun ColorSwatch(label: String, hex: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(Modifier.fillMaxWidth().height(34.dp), shape = RoundedCornerShape(10.dp), color = color, border = BorderStroke(1.dp, Color.White.copy(alpha = .12f))) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(hex, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun SurfaceSample(label: String, hex: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(Modifier.fillMaxWidth().height(27.dp), shape = RoundedCornerShape(8.dp), color = color, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(hex, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun StatusBadge(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier.height(48.dp), shape = MaterialTheme.shapes.small, color = tint.copy(alpha = .13f), border = BorderStroke(1.dp, tint.copy(alpha = .45f))) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = tint, maxLines = 1)
        }
    }
}
