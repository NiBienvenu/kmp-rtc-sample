package org.kmprtc.sample.call

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private val engine by lazy { PjsipCallEngine() }

actual fun createCallEngine(): CallEngine = engine

@Composable
actual fun RemoteVideo(modifier: Modifier) = VideoSurface(modifier, onTop = false, engine::attachRemote)

@Composable
actual fun LocalPreview(modifier: Modifier) = VideoSurface(modifier, onTop = true, engine::attachPreview)

/** Une SurfaceView Android dont la surface est donnee a PJSIP pour y dessiner la video. */
@Composable
private fun VideoSurface(modifier: Modifier, onTop: Boolean, attach: (android.view.Surface?) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                setZOrderMediaOverlay(onTop)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(h: SurfaceHolder) = Unit
                    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, height: Int) = attach(h.surface)
                    override fun surfaceDestroyed(h: SurfaceHolder) = attach(null)
                })
            }
        },
    )
}
