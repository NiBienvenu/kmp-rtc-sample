package org.kmprtc.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kmprtc.sample.call.Registration
import org.kmprtc.sample.ui.theme.CallColors

private val avatarPalette = listOf(
    Color(0xFF5B4BD1), Color(0xFF00897B), Color(0xFFD9480F), Color(0xFF1971C2),
    Color(0xFFC2255C), Color(0xFF2F9E44), Color(0xFF7048E8), Color(0xFFE67700),
)

fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Avatar rond avec initiales, couleur stable par nom. */
@Composable
fun Avatar(name: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val color = avatarPalette[(name.hashCode() and Int.MAX_VALUE) % avatarPalette.size]
    Box(
        modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.38f).sp)
    }
}

/** Pastille d'etat SIP affichee dans la barre du haut. */
@Composable
fun RegistrationChip(state: Registration, supported: Boolean) {
    val (label, color) = when {
        !supported -> "Appels : Android" to MaterialTheme.colorScheme.outline
        state == Registration.ONLINE -> "En ligne" to CallColors.online
        state == Registration.CONNECTING -> "Connexion..." to Color(0xFFE6A700)
        state == Registration.FAILED -> "Hors ligne" to MaterialTheme.colorScheme.error
        else -> "Hors ligne" to MaterialTheme.colorScheme.outline
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Ecran vide illustre (liste sans contenu). */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Titre de section en petites capitales. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m >= 60) "${m / 60}:${(m % 60).pad()}:${s.pad()}" else "${m.pad()}:${s.pad()}"
}

private fun Int.pad() = toString().padStart(2, '0')
