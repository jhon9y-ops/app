package com.p2p.client

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.webrtc.*

class MainActivity : AppCompatActivity() {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configura uma interface simples via código para os botões
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            // Corrigido de "padding = 50" para "setPadding" nativo do Android
            setPadding(50, 50, 50, 50)
        }

        val btnAcessibilidade = Button(this).apply {
            text = "Ativar Controle Remoto (Acessibilidade)"
            setOnClickListener {
                // Abre a tela de configurações do Android direto na aba de Acessibilidade
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val btnIniciarVideo = Button(this).apply {
            text = "Iniciar Transmissão de Vídeo"
            setOnClickListener {
                pedirPermissoesEIniciarWebRTC()
            }
        }

        layout.addView(btnAcessibilidade)
        layout.addView(btnIniciarVideo)
        setContentView(layout)

        // Inicializa a biblioteca nativa do WebRTC do Google
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
        )
    }

    private fun pedirPermissoesEIniciarWebRTC() {
        val permissoes = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        
        // Pede as permissões de câmera e microfone na tela do celular
        ActivityCompat.requestPermissions(this, permissoes, 100)
        
        inicializarMidiaLocal()
    }

    private fun inicializarMidiaLocal() {
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        val eglBase = EglBase.create()

        // Captura a câmera do celular
        val videoCapturer = criarCapturadorDeCamera()
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        
        val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast == true)
        videoCapturer?.initialize(surfaceTextureHelper, this, videoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30) // Resolução HD a 30 FPS

        // Cria os canais de áudio e vídeo que serão enviados para o seu computador
        localVideoTrack = peerConnectionFactory?.createVideoTrack("VIDEO_TRACK_ID", videoSource)
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("AUDIO_TRACK_ID", audioSource)
    }

    private fun criarCapturadorDeCamera(): VideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames

        // Tenta achar a câmera frontal primeiro, se não conseguir, usa a traseira
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }
}