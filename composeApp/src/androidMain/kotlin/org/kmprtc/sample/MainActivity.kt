package org.kmprtc.sample

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

/** Contexte applicatif pour le code Android qui n'a pas d'Activity sous la main (Prefs, PJSIP). */
object AppContext {
    private lateinit var context: Context
    fun init(ctx: Context) { context = ctx.applicationContext }
    fun get(): Context = context
}

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        // Micro et camera : necessaires avant le premier appel.
        permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        // Ecran allume et visible par-dessus l'ecran de verrouillage pendant un appel.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent { App() }
    }
}
