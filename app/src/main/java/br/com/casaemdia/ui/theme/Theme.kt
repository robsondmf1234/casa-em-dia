package br.com.casaemdia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Green40 = Color(0xFF2E7D32)
val Green80 = Color(0xFFA5D6A7)
val GreenContainer = Color(0xFFDDF3DE)
val WarningAmber = Color(0xFFF9A825)
val DangerRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF0B3D10),
    secondary = Color(0xFF52634F),
    background = Color(0xFFF7FBF5),
    onBackground = Color(0xFF191C19),
    surface = Color.White,
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFE6F0E3),
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF0B3D10),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = GreenContainer,
    secondary = Color(0xFFB8CCB4),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF1A1F1A),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF2B352B),
    error = Color(0xFFEF9A9A),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun CasaEmDiaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
