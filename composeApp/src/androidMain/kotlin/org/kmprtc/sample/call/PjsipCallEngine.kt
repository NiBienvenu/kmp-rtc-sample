package org.kmprtc.sample.call

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kmprtc.sample.AppContext
import org.kmprtc.sample.data.SipAccount
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AudioMedia
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.AuthCredInfoVector
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.CallVidSetStreamParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.IntVector
import org.pjsip.pjsua2.LogEntry
import org.pjsip.pjsua2.LogWriter
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.OnRegStateParam
import org.pjsip.pjsua2.SrtpOpt
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.VideoPreview
import org.pjsip.pjsua2.VideoPreviewOpParam
import org.pjsip.pjsua2.VideoWindow
import org.pjsip.pjsua2.VideoWindowHandle
import org.pjsip.pjsua2.pj_turn_tp_type
import org.pjsip.pjsua2.pjmedia_srtp_keying_method
import org.pjsip.pjsua2.pjmedia_srtp_use
import org.pjsip.pjsua2.pjmedia_orient
import org.pjsip.pjsua2.pjmedia_type
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_status_code
import org.pjsip.pjsua2.pjsip_transport_type_e
import org.pjsip.pjsua2.pjsua_call_media_status
import org.pjsip.pjsua2.pjsua_call_vid_strm_op
import org.pjsip.pjsua2.pjsua_stun_use
import java.util.concurrent.Executors

/**
 * Moteur d'appel Android base sur PJSUA2 (bibliotheque pjdroid).
 *
 * Regle PJSIP : tous les appels a la bibliotheque passent par UN seul thread ("pjsip"),
 * c'est le role de [pj]. Les callbacks (onCallState, onIncomingCall...) arrivent depuis
 * les threads internes de PJSIP et ne font que mettre a jour l'etat.
 */
class PjsipCallEngine : CallEngine {
    private companion object { const val TAG = "PjsipCallEngine" }

    private val pjDispatcher = Executors.newSingleThreadExecutor { Thread(it, "pjsip") }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + pjDispatcher)
    private suspend fun pj(block: () -> Unit) = withContext(pjDispatcher) { block() }

    override val supportsCalls = true
    private val _registration = MutableStateFlow(Registration.OFFLINE)
    override val registration = _registration.asStateFlow()
    private val _call = MutableStateFlow<ActiveCall?>(null)
    override val call = _call.asStateFlow()

    private val ep = Endpoint()
    private val logWriter = object : LogWriter() {
        override fun write(entry: LogEntry) {
            Log.println(if (entry.level <= 1) Log.ERROR else if (entry.level == 2) Log.WARN else Log.DEBUG, "PJSIP", entry.msg)
        }
    }
    private var started = false
    private var transportId = -1
    private var account: SipAccountImpl? = null
    @Volatile private var current: SipCall? = null

    // Media de l'appel en cours
    private var callAudio: AudioMedia? = null
    private var remoteWindow: VideoWindow? = null
    private var preview: VideoPreview? = null
    private var remoteSurface: Surface? = null
    private var previewSurface: Surface? = null
    private var ringtone: Ringtone? = null

    init {
        // La notification d'appel entrant disparait des que l'appel n'est plus "en train de sonner"
        // (accepte, refuse, annule par l'appelant ou termine), quel que soit le chemin pris.
        scope.launch {
            _call.collect { c ->
                if (c == null || !c.incoming || c.state != CallState.RINGING) CallService.hideIncoming(AppContext.get())
            }
        }
    }

    private val audioManager get() = AppContext.get().getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ─── Compte ───────────────────────────────────────────────────────────────

    override suspend fun start(account: SipAccount) = pj {
        if (!started) createEndpoint(account)
        // Garde l'appli en vie en arriere-plan pour recevoir les appels.
        CallService.start(AppContext.get())
        this.account?.let { runCatching { it.shutdown() } }
        _registration.value = Registration.CONNECTING
        this.account = SipAccountImpl().apply { create(accountConfig(account)) }
    }

    override suspend fun stop() = pj {
        account?.let { runCatching { it.setRegistration(false) }; it.shutdown() }
        account = null
        _registration.value = Registration.OFFLINE
        CallService.stop(AppContext.get())
    }

    private fun createEndpoint(acc: SipAccount) {
        ep.libCreate()
        val cfg = EpConfig()
        cfg.uaConfig.userAgent = "KMP-RTC-Sample"
        acc.stun?.let { cfg.uaConfig.stunServer.add(it) }
        // Les logs PJSIP vont sur stdout, invisible sur Android : on les envoie dans Logcat (tag "PJSIP").
        cfg.logConfig.writer = logWriter
        cfg.logConfig.level = 4
        cfg.logConfig.consoleLevel = 4
        cfg.medConfig.apply {
            val rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toLongOrNull() ?: 48000L
            clockRate = rate
            sndClockRate = rate
            ecTailLen = 200          // annulation d'echo
            jbMax = 400              // jitter buffer
        }
        ep.libInit(cfg)

        // Meme ordre de codecs que le serveur : pas de transcodage dans Asterisk.
        listOf("opus/48000/2" to 255, "G722/16000/1" to 200, "PCMU/8000/1" to 150, "PCMA/8000/1" to 140)
            .forEach { (c, p) -> runCatching { ep.codecSetPriority(c, p.toShort()) } }
        setupVideoCodecs()

        val transport = when (acc.transport.lowercase()) {
            "tcp" -> pjsip_transport_type_e.PJSIP_TRANSPORT_TCP
            "tls" -> pjsip_transport_type_e.PJSIP_TRANSPORT_TLS
            else -> pjsip_transport_type_e.PJSIP_TRANSPORT_UDP
        }
        transportId = ep.transportCreate(transport, TransportConfig().apply { port = 0 })
        ep.libStart()
        started = true
    }

    private fun accountConfig(acc: SipAccount) = AccountConfig().apply {
        idUri = "sip:${acc.username}@${acc.domain}"
        regConfig.registrarUri = "sip:${acc.domain}"
        sipConfig.transportId = transportId
        sipConfig.authCreds = AuthCredInfoVector().apply { add(AuthCredInfo("digest", "*", acc.username, 0, acc.password)) }

        // SRTP optionnel (SDES) : correspond a media_encryption=sdes cote Asterisk
        mediaConfig.srtpUse = pjmedia_srtp_use.PJMEDIA_SRTP_OPTIONAL
        mediaConfig.srtpSecureSignaling = 0
        mediaConfig.srtpOpt = SrtpOpt().apply {
            keyings = IntVector().apply { add(pjmedia_srtp_keying_method.PJMEDIA_SRTP_KEYING_SDES) }
        }

        // Video : on envoie la camera frontale et on affiche la video recue automatiquement
        videoConfig.autoTransmitOutgoing = true
        videoConfig.autoShowIncoming = true
        videoConfig.defaultCaptureDevice = frontCamera()

        // NAT : ICE + STUN + TURN (identifiants temporaires donnes par /sip/account)
        natConfig.apply {
            if (acc.stun != null) {
                sipStunUse = pjsua_stun_use.PJSUA_STUN_USE_DEFAULT
                mediaStunUse = pjsua_stun_use.PJSUA_STUN_USE_DEFAULT
            }
            iceEnabled = true
            contactRewriteUse = 1
            viaRewriteUse = 1
            sdpNatRewriteUse = 1
            udpKaIntervalSec = 15
            if (!acc.turn.isNullOrBlank() && !acc.turnUsername.isNullOrBlank()) {
                turnEnabled = true
                turnServer = acc.turn
                turnConnType = pj_turn_tp_type.PJ_TURN_TP_UDP
                turnUserName = acc.turnUsername
                turnPasswordType = 0
                turnPassword = acc.turnPassword
            }
        }
    }

    /**
     * Les identifiants des codecs video dependent du build (ex. "H264/97", "VP8/104") :
     * on les cherche par nom au lieu de les ecrire en dur. Les deux telephones doivent
     * avoir un codec commun, Asterisk ne transcode pas la video.
     */
    private fun setupVideoCodecs() {
        val codecs = runCatching { ep.videoCodecEnum2().map { it.codecId } }.getOrDefault(emptyList())
        Log.i(TAG, "Codecs video disponibles : $codecs")
        if (codecs.isEmpty()) Log.e(TAG, "Aucun codec video : la video ne marchera pas")
        codecs.forEach { id ->
            val prio = when {
                id.startsWith("H264", ignoreCase = true) -> 255
                id.startsWith("VP8", ignoreCase = true) -> 200
                else -> 0 // VP9 etc. : Asterisk ne les autorise pas
            }
            runCatching { ep.videoCodecSetPriority(id, prio.toShort()) }
            if (prio == 0) return@forEach
            // 640x480 a 15 i/s : supporte par tous les encodeurs materiels et leger pour le reseau.
            runCatching {
                val param = ep.getVideoCodecParam(id)
                param.encFmt.apply {
                    width = 640; height = 480; fpsNum = 15; fpsDenum = 1
                    avgBps = 512_000; maxBps = 1_024_000
                }
                param.ignoreFmtp = false
                ep.setVideoCodecParam(id, param)
            }.onFailure { Log.w(TAG, "Parametres $id : ${it.message}") }
        }
    }

    private fun frontCamera(): Int = runCatching {
        val mgr = ep.vidDevManager()
        (0 until mgr.devCount.toInt()).firstOrNull { mgr.getDevInfo(it).name.contains("front", ignoreCase = true) }
    }.getOrNull() ?: -1 // -1 = camera par defaut

    // ─── Appels ───────────────────────────────────────────────────────────────

    override suspend fun dial(sipUri: String, video: Boolean) = pj {
        val acc = account ?: return@pj
        if (current != null) return@pj
        val c = SipCall(acc, -1)
        current = c
        _call.value = ActiveCall(sipUri, incoming = false, state = CallState.CALLING, video = video, speaker = video)
        routeAudio(speaker = video)
        try {
            c.makeCall(sipUri, CallOpParam(true).apply { opt.audioCount = 1; opt.videoCount = if (video) 1 else 0 })
        } catch (t: Throwable) {
            finish("Echec : ${t.message}")
        }
    }

    override suspend fun answer(video: Boolean) = pj {
        val c = current ?: return@pj
        stopRingtone()
        CallService.hideIncoming(AppContext.get())
        _call.update { it?.copy(video = video, cameraOn = video, speaker = video, state = CallState.CONNECTING) }
        routeAudio(speaker = video)
        c.answer(CallOpParam(true).apply {
            statusCode = pjsip_status_code.PJSIP_SC_OK
            opt.audioCount = 1
            opt.videoCount = if (video) 1 else 0
        })
    }

    override suspend fun hangup() = pj {
        val c = current ?: return@pj
        val ringing = _call.value?.let { it.incoming && it.state == CallState.RINGING } == true
        runCatching {
            c.hangup(CallOpParam().apply {
                statusCode = if (ringing) pjsip_status_code.PJSIP_SC_DECLINE else pjsip_status_code.PJSIP_SC_OK
            })
        }
        finish("Appel termine")
    }

    override suspend fun setMuted(muted: Boolean) = pj {
        val audio = callAudio
        if (audio != null) {
            val mic = ep.audDevManager().captureDevMedia
            if (muted) mic.stopTransmit(audio) else mic.startTransmit(audio)
        }
        _call.update { it?.copy(muted = muted) }
    }

    override suspend fun setSpeaker(on: Boolean) = pj {
        audioManager.isSpeakerphoneOn = on
        _call.update { it?.copy(speaker = on) }
    }

    override suspend fun setCamera(on: Boolean) = pj {
        val op = if (on) pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT
        else pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_STOP_TRANSMIT
        runCatching { current?.vidSetStream(op, CallVidSetStreamParam()) }
        _call.update { it?.copy(cameraOn = on) }
    }

    override suspend fun sendDtmf(digits: String) = pj {
        runCatching { current?.dialDtmf(digits) }
    }

    // ─── Video : surfaces donnees par RemoteVideo / LocalPreview ──────────────

    fun attachRemote(surface: Surface?) {
        // Destruction : synchrone, la surface ne doit plus etre utilisee apres le retour.
        if (surface == null) runBlocking(pjDispatcher) { remoteSurface = null; bindRemote() }
        else scope.launch { remoteSurface = surface; bindRemote() }
    }

    fun attachPreview(surface: Surface?) {
        if (surface == null) runBlocking(pjDispatcher) { previewSurface = null; bindPreview() }
        else scope.launch { previewSurface = surface; bindPreview() }
    }

    private fun bindRemote() {
        val win = remoteWindow ?: return
        runCatching { win.setWindow(VideoWindowHandle().apply { handle.setWindow(remoteSurface) }) }
            .onFailure { Log.e(TAG, "Video distante : ${it.message}") }
    }

    private fun bindPreview() {
        val prev = preview ?: return
        runCatching {
            val surface = previewSurface
            if (surface == null) prev.stop()
            else prev.start(VideoPreviewOpParam().apply { window.handle.setWindow(surface) })
        }.onFailure { Log.e(TAG, "Apercu camera : ${it.message}") }
    }

    // ─── Outils ───────────────────────────────────────────────────────────────

    private fun routeAudio(speaker: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = speaker
    }

    private fun finish(reason: String) {
        stopRingtone()
        CallService.hideIncoming(AppContext.get())
        runCatching { preview?.stop() }
        // Les objets PJSIP se liberent sur le thread pjsip, jamais dans un callback.
        val old = listOfNotNull(current, preview, remoteWindow)
        preview = null
        remoteWindow = null
        callAudio = null
        current = null
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        val ended = _call.value ?: return
        _call.value = ended.copy(state = CallState.ENDED, endReason = reason)
        // Laisse "Appel termine" visible un instant puis ferme l'ecran d'appel.
        scope.launch {
            delay(1500)
            _call.update { if (it?.state == CallState.ENDED) null else it }
            old.forEach {
                runCatching {
                    when (it) {
                        is Call -> it.delete()
                        is VideoPreview -> it.delete()
                        is VideoWindow -> it.delete()
                    }
                }
            }
        }
    }

    private fun startRingtone() {
        val ctx = AppContext.get()
        ringtone = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        runCatching { ringtone?.play() }
    }

    private fun stopRingtone() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    // ─── Callbacks PJSIP ──────────────────────────────────────────────────────

    private inner class SipAccountImpl : Account() {
        override fun onRegState(prm: OnRegStateParam) {
            _registration.value = if (prm.code / 100 == 2 && prm.expiration > 0) Registration.ONLINE else Registration.FAILED
        }

        override fun onIncomingCall(prm: OnIncomingCallParam) {
            val c = SipCall(this, prm.callId)
            scope.launch {
                if (current != null) {
                    // Deja en appel : occupe
                    runCatching { c.hangup(CallOpParam().apply { statusCode = pjsip_status_code.PJSIP_SC_BUSY_HERE }) }
                    return@launch
                }
                current = c
                val info = c.info
                val video = info.remVideoCount > 0
                _call.value = ActiveCall(info.remoteUri, incoming = true, state = CallState.RINGING, video = video)
                c.answer(CallOpParam().apply { statusCode = pjsip_status_code.PJSIP_SC_RINGING })
                startRingtone()
                // Appli en arriere-plan ou ecran eteint : la notification ouvre l'ecran d'appel.
                CallService.showIncoming(AppContext.get(), sipUser(info.remoteUri), video)
            }
        }
    }

    private inner class SipCall(acc: Account, id: Int) : Call(acc, id) {
        override fun onCallState(prm: OnCallStateParam) {
            if (this !== current) return
            val info = runCatching { info }.getOrNull() ?: return
            when (info.state) {
                pjsip_inv_state.PJSIP_INV_STATE_EARLY ->
                    _call.update { if (it?.incoming == false) it.copy(state = CallState.RINGING) else it }
                pjsip_inv_state.PJSIP_INV_STATE_CONNECTING ->
                    _call.update { it?.copy(state = CallState.CONNECTING) }
                pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED ->
                    _call.update { it?.copy(state = CallState.CONNECTED) }
                pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                    val reason = when (info.lastStatusCode) {
                        pjsip_status_code.PJSIP_SC_BUSY_HERE -> "Occupe"
                        pjsip_status_code.PJSIP_SC_DECLINE -> "Appel refuse"
                        pjsip_status_code.PJSIP_SC_NOT_FOUND, pjsip_status_code.PJSIP_SC_TEMPORARILY_UNAVAILABLE -> "Indisponible"
                        pjsip_status_code.PJSIP_SC_REQUEST_TIMEOUT -> "Pas de reponse"
                        else -> "Appel termine"
                    }
                    scope.launch { if (this@SipCall === current) finish(reason) }
                }
                else -> Unit
            }
        }

        override fun onCallMediaState(prm: OnCallMediaStateParam) {
            val info = runCatching { info }.getOrNull() ?: return
            scope.launch {
                info.media.forEachIndexed { i, m ->
                    if (m.status != pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) return@forEachIndexed
                    when (m.type) {
                        pjmedia_type.PJMEDIA_TYPE_AUDIO -> {
                            // Brancher le son : micro -> appel, appel -> haut-parleur
                            val audio = getAudioMedia(i)
                            val dev = ep.audDevManager()
                            audio.startTransmit(dev.playbackDevMedia)
                            if (_call.value?.muted != true) dev.captureDevMedia.startTransmit(audio)
                            callAudio = audio
                        }
                        pjmedia_type.PJMEDIA_TYPE_VIDEO -> {
                            if (m.videoIncomingWindowId >= 0) {
                                remoteWindow = VideoWindow(m.videoIncomingWindowId)
                                bindRemote()
                            }
                            Log.i(TAG, "Video active : fenetre=${m.videoIncomingWindowId} camera=${m.videoCapDev}")
                            if (m.videoCapDev >= 0) {
                                // Portrait : sans ca l'image envoyee est couchee.
                                runCatching {
                                    ep.vidDevManager().setCaptureOrient(m.videoCapDev, pjmedia_orient.PJMEDIA_ORIENT_ROTATE_270DEG, true)
                                }
                            }
                            if (m.videoCapDev >= 0 && preview == null) {
                                preview = VideoPreview(m.videoCapDev)
                                bindPreview()
                            }
                        }
                    }
                }
            }
        }
    }
}
