package com.p2p.client

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class RemoteControlService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var session: DefaultClientWebSocketSession? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        conectarAoServidorControlador()
    }

    private fun conectarAoServidorControlador() {
        val client = HttpClient {
            install(WebSockets)
        }

        serviceScope.launch {
            try {
                client.webSocket(host = "10.0.2.2", port = 8081, path = "/rtc/sala-1") {
                    session = this
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            processarComandoRemoto(frame.readText())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                delay(5000)
                conectarAoServidorControlador()
            }
        }
    }

    private fun processarComandoRemoto(jsonTexto: String) {
        try {
            val json = Json.parseToJsonElement(jsonTexto).jsonObject
            val tipo = json["type"]?.jsonPrimitive?.content

            Handler(Looper.getMainLooper()).post {
                when (tipo) {
                    "CLICK" -> {
                        val x = json["x"]?.jsonPrimitive?.float ?: 0f
                        val y = json["y"]?.jsonPrimitive?.float ?: 0f
                        simularGestoNaTela(x, y, x, y, 50) // Toque rápido de 50ms
                    }
                    "LONG_CLICK" -> {
                        val x = json["x"]?.jsonPrimitive?.float ?: 0f
                        val y = json["y"]?.jsonPrimitive?.float ?: 0f
                        simularGestoNaTela(x, y, x, y, 1000) // Mantém pressionado por 1 segundo
                    }
                    "SWIPE" -> {
                        // Arrastar livre com coordenadas de início e fim (essencial para mapas do Uber)
                        val xInicio = json["xInicio"]?.jsonPrimitive?.float ?: 0f
                        val yInicio = json["yInicio"]?.jsonPrimitive?.float ?: 0f
                        val xFim = json["xFim"]?.jsonPrimitive?.float ?: 0f
                        val yFim = json["yFim"]?.jsonPrimitive?.float ?: 0f
                        val duracao = json["duration"]?.jsonPrimitive?.long ?: 300L
                        simularGestoNaTela(xInicio, yInicio, xFim, yFim, duracao)
                    }
                    "SCROLL" -> {
                        val direcao = json["direction"]?.jsonPrimitive?.content ?: "DOWN"
                        executarRolagemPadrao(direcao)
                    }
                    "TEXTO" -> {
                        val textoParaDigitar = json["text"]?.jsonPrimitive?.content ?: ""
                        injetarTextoNoCampoFocado(textoParaDigitar)
                    }
                    "SYSTEM_KEY" -> {
                        // Controla os botões físicos do sistema Android
                        val chave = json["key"]?.jsonPrimitive?.content ?: ""
                        executarBotaoDoSistema(chave)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Mecanismo universal de injeção de gestos na tela (Toques, Cliques Longos e Arrastar)
    private fun simularGestoNaTela(xIni: Float, yIni: Float, xFim: Float, yFim: Float, tempo: Long) {
        val caminho = Path().apply {
            moveTo(xIni, yIni)
            if (xIni != xFim || yIni != yFim) {
                lineTo(xFim, yFim)
            }
        }
        val gesto = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(caminho, 0, tempo))
            .build()
        dispatchGesture(gesto, null, null)
    }

    private fun executarRolagemPadrao(direcao: String) {
        val metrics = resources.displayMetrics
        val largura = metrics.widthPixels.toFloat()
        val altura = metrics.heightPixels.toFloat()
        val xCentro = largura / 2f

        if (direcao == "DOWN") {
            simularGestoNaTela(xCentro, altura * 0.8f, xCentro, altura * 0.2f, 300)
        } else {
            simularGestoNaTela(xCentro, altura * 0.2f, xCentro, altura * 0.8f, 300)
        }
    }

    private fun injetarTextoNoCampoFocado(texto: String) {
        val noFocado = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (noFocado != null) {
            val argumentos = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, texto)
            }
            noFocado.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, argumentos)
            noFocado.recycle()
        }
    }

    private fun executarBotaoDoSistema(chave: String) {
        when (chave) {
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)             // Voltar tela
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)             // Ir para início
            "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)       // Aplicativos abertos
            "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) // Abrir barra de notificações
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
