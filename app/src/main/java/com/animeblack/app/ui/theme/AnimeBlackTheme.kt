package com.animeblack.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Static brand palette. Dynamic system colors are intentionally not used. */
object AnimeBlackPalette {
    val Background = Color(0xFF07090D)
    val Surface = Color(0xFF0D1117)
    val SurfaceRaised = Color(0xFF141B24)
    val SurfaceHighest = Color(0xFF1B2430)
    val Primary = Color(0xFF00A3FF)
    val OnPrimary = Color(0xFF001B2C)
    val PrimaryContainer = Color(0xFF003653)
    val OnPrimaryContainer = Color(0xFFC9E9FF)
    val Secondary = Color(0xFFB69CFF)
    val OnSecondary = Color(0xFF25134E)
    val SecondaryContainer = Color(0xFF3D2A66)
    val OnSecondaryContainer = Color(0xFFEBDDFF)
    val Tertiary = Color(0xFF66D9C8)
    val OnTertiary = Color(0xFF003730)
    val TertiaryContainer = Color(0xFF14564E)
    val OnTertiaryContainer = Color(0xFFB5F2E8)
    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFFB9C2CE)
    val Outline = Color(0xFF8B96A3)
    val OutlineVariant = Color(0xFF3C4652)
    val Error = Color(0xFFFF6B78)
    val OnError = Color(0xFF4B0010)
    val ErrorContainer = Color(0xFF69001B)
    val OnErrorContainer = Color(0xFFFFD9DD)
    val Success = Color(0xFF67D99A)
    val Warning = Color(0xFFFFC36B)
    val Info = Primary
}

val AnimeBlackDarkColorScheme = darkColorScheme(
    primary = AnimeBlackPalette.Primary,
    onPrimary = AnimeBlackPalette.OnPrimary,
    primaryContainer = AnimeBlackPalette.PrimaryContainer,
    onPrimaryContainer = AnimeBlackPalette.OnPrimaryContainer,
    secondary = AnimeBlackPalette.Secondary,
    onSecondary = AnimeBlackPalette.OnSecondary,
    secondaryContainer = AnimeBlackPalette.SecondaryContainer,
    onSecondaryContainer = AnimeBlackPalette.OnSecondaryContainer,
    tertiary = AnimeBlackPalette.Tertiary,
    onTertiary = AnimeBlackPalette.OnTertiary,
    tertiaryContainer = AnimeBlackPalette.TertiaryContainer,
    onTertiaryContainer = AnimeBlackPalette.OnTertiaryContainer,
    background = AnimeBlackPalette.Background,
    onBackground = AnimeBlackPalette.TextPrimary,
    surface = AnimeBlackPalette.Surface,
    onSurface = AnimeBlackPalette.TextPrimary,
    surfaceVariant = AnimeBlackPalette.SurfaceRaised,
    onSurfaceVariant = AnimeBlackPalette.TextSecondary,
    surfaceTint = AnimeBlackPalette.Primary,
    inverseSurface = Color(0xFFE1E6EC),
    inverseOnSurface = Color(0xFF222830),
    inversePrimary = Color(0xFF00639C),
    error = AnimeBlackPalette.Error,
    onError = AnimeBlackPalette.OnError,
    errorContainer = AnimeBlackPalette.ErrorContainer,
    onErrorContainer = AnimeBlackPalette.OnErrorContainer,
    outline = AnimeBlackPalette.Outline,
    outlineVariant = AnimeBlackPalette.OutlineVariant,
    scrim = Color.Black
)

data class AnimeBlackStatusColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color
)

val LocalAnimeBlackStatusColors = staticCompositionLocalOf {
    AnimeBlackStatusColors(
        success = AnimeBlackPalette.Success,
        warning = AnimeBlackPalette.Warning,
        error = AnimeBlackPalette.Error,
        info = AnimeBlackPalette.Info
    )
}

object AnimeBlackSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    val minTouchTarget = 48.dp
    val iconSmall = 18.dp
    val icon = 24.dp
    val iconLarge = 32.dp
    val hairline = 1.dp
}

val AnimeBlackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Anime Black's prototype Material 3 theme. It is intentionally only used by the
 * design-system preview until the visual review gate is approved.
 */
@Composable
fun AnimeBlackTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAnimeBlackStatusColors provides AnimeBlackStatusColors(
        success = AnimeBlackPalette.Success,
        warning = AnimeBlackPalette.Warning,
        error = AnimeBlackPalette.Error,
        info = AnimeBlackPalette.Info
    )) {
        MaterialTheme(
            colorScheme = AnimeBlackDarkColorScheme,
            typography = AnimeBlackTypography,
            shapes = AnimeBlackShapes,
            content = content
        )
    }
}
