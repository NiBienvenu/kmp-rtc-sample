package org.kmprtc.sample.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.kmprtc.sample.AppViewModel
import org.kmprtc.sample.call.ActiveCall
import org.kmprtc.sample.call.CallState
import org.kmprtc.sample.call.LocalPreview
import org.kmprtc.sample.call.RemoteVideo
import org.kmprtc.sample.call.sipUser
import org.kmprtc.sample.ui.components.Avatar
import org.kmprtc.sample.ui.components.formatDuration
import org.kmprtc.sample.ui.theme.CallColors

@Composable
fun CallScreen(call: ActiveCall, vm: AppViewModel) {
    val number = sipUser(call.remoteUri)
    val name = vm.nameOf(number)
    val isRoom = number.length == 5 && number.startsWith("8")
    val showVideo = call.video && call.state == CallState.CONNECTED
    var seconds by remember { mutableIntStateOf(0) }
    var keypad by remember { mutableStateOf(false) }

    LaunchedEffect(call.state) {
        if (call.state == CallState.CONNECTED) while (true) { delay(1000); seconds++ }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF2A1F6B), CallColors.stage, CallColors.stage))
        )
    ) {
        if (showVideo) {
            RemoteVideo(Modifier.fillMaxSize())
            if (call.cameraOn) {
                LocalPreview(
                    Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopEnd)
                        .width(110.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp))
                )
            }
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showVideo) {
                Text(name, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                Text(formatDuration(seconds), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.Start))
            } else {
                Spacer(Modifier.height(64.dp))
                PulsingAvatar(name, pulsing = call.state != CallState.CONNECTED && call.state != CallState.ENDED)
                Spacer(Modifier.height(24.dp))
                Text(name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    statusText(call, seconds, isRoom),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(Modifier.weight(1f))

            if (keypad) Keypad(onKey = vm::dtmf, onClose = { keypad = false })

            when {
                call.state == CallState.ENDED -> Unit
                call.incoming && call.state == CallState.RINGING -> IncomingActions(call, vm)
                else -> InCallActions(call, vm, onKeypad = { keypad = !keypad })
            }
        }
    }
}

private fun statusText(call: ActiveCall, seconds: Int, isRoom: Boolean): String = when (call.state) {
    CallState.CALLING -> if (isRoom) "Entree dans la salle..." else "Appel en cours..."
    CallState.RINGING -> if (call.incoming) "Appel ${if (call.video) "video" else "audio"} entrant" else "Ca sonne..."
    CallState.CONNECTING -> "Connexion..."
    CallState.CONNECTED -> formatDuration(seconds)
    CallState.ENDED -> call.endReason ?: "Appel termine"
}

@Composable
private fun PulsingAvatar(name: String, pulsing: Boolean) {
    val t = rememberInfiniteTransition()
    val scale by t.animateFloat(1f, if (pulsing) 1.18f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse))
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(150.dp).scale(scale).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Avatar(name, 120.dp)
    }
}

@Composable
private fun IncomingActions(call: ActiveCall, vm: AppViewModel) {
    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        RoundAction(Icons.Rounded.CallEnd, "Refuser", CallColors.hangup, big = true) { vm.hangup() }
        if (call.video) RoundAction(Icons.Rounded.Videocam, "Video", CallColors.accept, big = true) { vm.answer(video = true) }
        RoundAction(Icons.Rounded.Call, "Repondre", CallColors.accept, big = true) { vm.answer(video = false) }
    }
}

@Composable
private fun InCallActions(call: ActiveCall, vm: AppViewModel, onKeypad: () -> Unit) {
    val glass = Color.White.copy(alpha = 0.14f)
    Surface(shape = RoundedCornerShape(32.dp), color = Color.Black.copy(alpha = 0.35f)) {
        Column(Modifier.padding(vertical = 18.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RoundAction(if (call.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, "Micro", if (call.muted) Color.White else glass, dark = call.muted) { vm.toggleMute() }
                RoundAction(if (call.speaker) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeDown, "Haut-parleur", if (call.speaker) Color.White else glass, dark = call.speaker) { vm.toggleSpeaker() }
                if (call.video) {
                    RoundAction(if (call.cameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, "Camera", if (!call.cameraOn) Color.White else glass, dark = !call.cameraOn) { vm.toggleCamera() }
                }
                RoundAction(Icons.Rounded.Dialpad, "Clavier", glass, onClick = onKeypad)
            }
            Spacer(Modifier.height(18.dp))
            RoundAction(Icons.Rounded.CallEnd, "Raccrocher", CallColors.hangup, big = true) { vm.hangup() }
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    color: Color,
    big: Boolean = false,
    dark: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, shape = CircleShape, color = color, modifier = Modifier.size(if (big) 72.dp else 58.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = if (dark) Color.Black else Color.White, modifier = Modifier.size(if (big) 32.dp else 26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

/** Clavier DTMF : sert a taper le PIN d'une salle de conference (*1 micro, *0 quitter). */
@Composable
private fun Keypad(onKey: (String) -> Unit, onClose: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 16.dp)) {
        Text(typed.ifEmpty { " " }, color = Color.White, fontSize = 26.sp, letterSpacing = 4.sp)
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 10.dp)) {
                row.forEach { c ->
                    Surface(
                        onClick = { typed += c; onKey(c.toString()) },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(62.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(c.toString(), color = Color.White, fontSize = 24.sp) }
                    }
                }
            }
        }
        TextButton(onClick = onClose) { Text("Masquer le clavier", color = Color.White) }
    }
}
