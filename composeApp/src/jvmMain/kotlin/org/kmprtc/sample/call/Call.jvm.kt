package org.kmprtc.sample.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Desktop : brancher ici PJSUA2 Java (JNI) ou Linphone SDK pour passer de vrais appels.
actual fun createCallEngine(): CallEngine = NoopCallEngine()

@Composable
actual fun RemoteVideo(modifier: Modifier) = Unit

@Composable
actual fun LocalPreview(modifier: Modifier) = Unit
