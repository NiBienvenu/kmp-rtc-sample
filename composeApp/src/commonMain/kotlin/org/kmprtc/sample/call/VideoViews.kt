package org.kmprtc.sample.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Video du correspondant (plein ecran pendant un appel video). */
@Composable
expect fun RemoteVideo(modifier: Modifier = Modifier)

/** Apercu de votre camera (petite vignette). */
@Composable
expect fun LocalPreview(modifier: Modifier = Modifier)
