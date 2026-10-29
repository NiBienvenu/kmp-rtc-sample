package org.kmprtc.sample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.kmprtc.sample.call.CallService
import org.kmprtc.sample.call.CallState
import org.kmprtc.sample.call.createCallEngine

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
        // Notifications (Android 13+) : sans elles, pas d'alerte d'appel entrant en arriere-plan.
        val wanted = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA) +
            if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        permissions.launch(wanted.toTypedArray())
        askBatteryExemption()
        // Ecran allume et visible par-dessus l'ecran de verrouillage pendant un appel.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent { App() }
        handleAnswer(intent)
    }

    // launchMode singleTask : un clic sur la notification arrive ici si l'appli est deja ouverte.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAnswer(intent)
    }

    /** Bouton "Repondre" de la notification : on decroche (en video si l'appel est video). */
    private fun handleAnswer(intent: Intent?) {
        if (intent?.getBooleanExtra(CallService.EXTRA_ANSWER, false) != true) return
        intent.removeExtra(CallService.EXTRA_ANSWER)
        val engine = createCallEngine()
        val call = engine.call.value ?: return
        if (call.incoming && call.state == CallState.RINGING) lifecycleScope.launch { engine.answer(call.video) }
    }

    /** Sans cette exemption, le mode Doze coupe le reseau et l'enregistrement SIP expire. */
    @SuppressLint("BatteryLife")
    private fun askBatteryExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
    }
}
