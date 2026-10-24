package org.kmprtc.sample.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kmp_rtc_sample.composeapp.generated.resources.Res
import kmp_rtc_sample.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.kmprtc.sample.API_BASE_URL
import org.kmprtc.sample.AppViewModel
import org.kmprtc.sample.UiState
import org.kmprtc.sample.ui.theme.CallColors

@Composable
fun AuthScreen(state: UiState, vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val isRegister = tab == 1
    val canSubmit = email.contains("@") && password.length >= 6 && (!isRegister || name.isNotBlank())

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to MaterialTheme.colorScheme.primaryContainer,
                0.45f to MaterialTheme.colorScheme.surface,
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Image(
                painterResource(Res.drawable.logo),
                contentDescription = "KMP RTC",
                modifier = Modifier.size(150.dp).clip(RoundedCornerShape(32.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Appels audio, video et conferences\nsur votre propre serveur",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            ServerChip(state.serverUp)
            Spacer(Modifier.height(28.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Connexion", "Inscription").forEachIndexed { i, label ->
                            SegmentedButton(
                                selected = tab == i,
                                onClick = { tab = i },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                            ) { Text(label) }
                        }
                    }
                    AnimatedVisibility(isRegister) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Field(name, { name = it }, "Nom affiche", Icons.Outlined.Badge)
                            Field(phone, { phone = it }, "Telephone", Icons.Outlined.Phone, KeyboardType.Phone)
                        }
                    }
                    Field(email, { email = it }, "Email", Icons.Outlined.Email, KeyboardType.Email)
                    PasswordField(password) { password = it }

                    Button(
                        onClick = {
                            if (isRegister) vm.register(name, email, phone, password) else vm.login(email, password)
                        },
                        enabled = canSubmit && !state.loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (isRegister) "Creer mon compte" else "Se connecter", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Chaque compte recoit un numero SIP a 7 chiffres.\nTous les telephones relies au meme serveur peuvent s'appeler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ServerChip(up: Boolean?) {
    val color = when (up) {
        true -> CallColors.online
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.outline
    }
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Dns, null, Modifier.size(16.dp), tint = color)
            Spacer(Modifier.size(6.dp))
            Text(API_BASE_URL, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Mot de passe (6+ caracteres)") },
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Afficher")
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}
