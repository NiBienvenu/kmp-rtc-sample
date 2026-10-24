package org.kmprtc.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette Material 3 generee a partir d'un violet de marque (#6750A4 proche).
private val Light = lightColorScheme(
    primary = Color(0xFF5B4BD1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF16065F),
    secondary = Color(0xFF5E5C71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E0F9),
    onSecondaryContainer = Color(0xFF1B192C),
    tertiary = Color(0xFF00696E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9CF0F5),
    onTertiaryContainer = Color(0xFF002022),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF48454E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFEBE6EE),
    surfaceContainerHighest = Color(0xFFE5E1E9),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFC9C4D0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF2B1A9E),
    primaryContainer = Color(0xFF4332B8),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC7C4DD),
    onSecondary = Color(0xFF302E42),
    secondaryContainer = Color(0xFF464559),
    onSecondaryContainer = Color(0xFFE3E0F9),
    tertiary = Color(0xFF80D4D9),
    onTertiary = Color(0xFF00373A),
    tertiaryContainer = Color(0xFF004F53),
    onTertiaryContainer = Color(0xFF9CF0F5),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C4D0),
    surfaceContainerLowest = Color(0xFF0E0D13),
    surfaceContainerLow = Color(0xFF1C1B20),
    surfaceContainer = Color(0xFF201F25),
    surfaceContainerHigh = Color(0xFF2A292F),
    surfaceContainerHighest = Color(0xFF35343A),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF48454E),
)

/** Couleurs fixes des boutons d'appel (identiques en clair et en sombre). */
object CallColors {
    val accept = Color(0xFF1E9E5A)
    val hangup = Color(0xFFE5484D)
    val online = Color(0xFF2EB872)
    val stage = Color(0xFF0F0D1A)
}

@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
