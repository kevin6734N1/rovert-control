package com.rovert.rovercontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Paleta de alto contraste sobre fondo oscuro: la app se usa a pleno sol y
 * con guantes, así que los colores primarios son vivos y los textos claros.
 */
private val RoverColorScheme = darkColorScheme(
    primary = Color(0xFF3DD6C4),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF9BF7E7),
    secondary = Color(0xFFA9C7FF),
    onSecondary = Color(0xFF00325B),
    secondaryContainer = Color(0xFF1B3A5C),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = Color(0xFFFFC46B),
    onTertiary = Color(0xFF3A2500),
    background = Color(0xFF070A0F),
    onBackground = Color(0xFFE7EEF7),
    surface = Color(0xFF10161E),
    onSurface = Color(0xFFE7EEF7),
    surfaceVariant = Color(0xFF1B232D),
    onSurfaceVariant = Color(0xFFBECBD9),
    outline = Color(0xFF64758A),
    outlineVariant = Color(0xFF2B3541),
    error = Color(0xFFFF5A5A),
    onError = Color(0xFF3B0007),
    errorContainer = Color(0xFF5C1016),
    onErrorContainer = Color(0xFFFFDAD8),
)

/**
 * Tipografía con cuerpos grandes y pesos altos para mejorar la legibilidad.
 */
private val RoverTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    ),
)

/**
 * Tema de la app. Siempre es oscuro y de alto contraste.
 */
@Composable
fun RoverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoverColorScheme,
        typography = RoverTypography,
        content = content,
    )
}
