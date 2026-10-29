package org.kmprtc.sample.call

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private val engine by lazy { PjsipCallEngine() }

actual fun createCallEngine(): CallEngine = engine

@Composable
actual fun RemoteVideo(modifier: Modifier) = VideoSurface(modifier, onTop = false, engine::attachRemote)

@Composable
actual fun LocalPreview(modifier: Modifier) = VideoSurface(modifier, onTop = true, engine::attachPreview)

/**
 * Une TextureView dont la surface est donnee a PJSIP pour y dessiner la video.
 * Pas de SurfaceView : elle s'affiche derriere la fenetre et le fond de l'ecran d'appel la cachait.
 */
@Composable
private fun VideoSurface(modifier: Modifier, @Suppress("UNUSED_PARAMETER") onTop: Boolean, attach: (Surface?) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var surface: Surface? = null
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        surface = Surface(st).also(attach)
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        attach(null)
                        surface?.release()
                        surface = null
                        return true
                    }
                }
            }
        },
    )
}
