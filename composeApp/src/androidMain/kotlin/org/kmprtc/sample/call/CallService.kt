package org.kmprtc.sample.call

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kmprtc.sample.AppContext
import org.kmprtc.sample.MainActivity

/**
 * Service au premier plan : garde le processus (et donc l'enregistrement SIP) en vie
 * quand l'appli est en arriere-plan ou l'ecran eteint. Sans lui, Android coupe le
 * reseau et les appels entrants n'arrivent plus.
 *
 * Il affiche aussi la notification d'appel entrant (plein ecran sur l'ecran verrouille).
 */
class CallService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DECLINE) {
            CoroutineScope(Dispatchers.Default).launch { createCallEngine().hangup() }
            return START_STICKY
        }
        goForeground()
        if (wakeLock == null) {
            // Garde le CPU eveille pour les keep-alive SIP (l'ecran peut s'eteindre).
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kmprtc:sip")
                .apply { setReferenceCounted(false); acquire() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { wakeLock?.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun goForeground() {
        val notif = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("KMP RTC")
            .setContentText("Pret a recevoir des appels")
            .setOngoing(true)
            .setContentIntent(openApp(this, 0))
            .build()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return startForeground(ID_SERVICE, notif)
        // phoneCall seul si le micro n'est pas autorise (sinon Android refuse le demarrage).
        val micOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val phone = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
        val types = if (micOk) phone or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else phone
        runCatching { startForeground(ID_SERVICE, notif, types) }
            .onFailure { startForeground(ID_SERVICE, notif, phone) }
    }

    companion object {
        private const val CHANNEL_SERVICE = "sip_service"
        private const val CHANNEL_CALLS = "incoming_calls"
        private const val ID_SERVICE = 1
        private const val ID_INCOMING = 2
        private const val ACTION_DECLINE = "org.kmprtc.sample.DECLINE"

        fun start(ctx: Context) {
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, CallService::class.java)) }
        }

        fun stop(ctx: Context) {
            hideIncoming(ctx)
            ctx.stopService(Intent(ctx, CallService::class.java))
        }

        /** Notification d'appel entrant : sonne et ouvre l'appli par-dessus l'ecran verrouille. */
        fun showIncoming(ctx: Context, caller: String, video: Boolean) {
            createChannels(ctx)
            val decline = PendingIntent.getService(
                ctx, 2, Intent(ctx, CallService::class.java).setAction(ACTION_DECLINE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val open = openApp(ctx, 1)
            val notif = NotificationCompat.Builder(ctx, CHANNEL_CALLS)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(if (video) "Appel video entrant" else "Appel entrant")
                .setContentText(caller)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000) // filet de securite si l'appel n'est jamais nettoye
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .addAction(0, "Refuser", decline)
                .addAction(0, "Repondre", openApp(ctx, 3, answer = true))
                .build()
            runCatching { ctx.getSystemService(NotificationManager::class.java).notify(ID_INCOMING, notif) }
        }

        fun hideIncoming(ctx: Context) {
            runCatching { ctx.getSystemService(NotificationManager::class.java).cancel(ID_INCOMING) }
        }

        /** Extra lu par MainActivity : decrocher tout de suite (bouton "Repondre" de la notification). */
        const val EXTRA_ANSWER = "org.kmprtc.sample.ANSWER"

        private fun openApp(ctx: Context, code: Int, answer: Boolean = false) = PendingIntent.getActivity(
            ctx, code,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_ANSWER, answer),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun createChannels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SERVICE, "Connexion SIP", NotificationManager.IMPORTANCE_LOW),
            )
            // Pas de son ici : la sonnerie est jouee par le moteur d'appel.
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_CALLS, "Appels entrants", NotificationManager.IMPORTANCE_HIGH)
                    .apply { setSound(null, null) },
            )
        }
    }
}
