package com.p2p.server

import com.p2p.shared.SignalPayload
import com.p2p.shared.SignalType
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

val rooms = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

fun main() {
    // Configurado para rodar na porta 8081 para não conflitar com o Frontend
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }

        routing {
            webSocket("/rtc/{roomId}") {
                val roomId = call.parameters["roomId"] ?: return@webSocket close()
                val sessionSet = rooms.computeIfAbsent(roomId) { Collections.synchronizedSet(LinkedHashSet()) }
                sessionSet.add(this)

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            // Repassa a mensagem SDP/ICE para os outros participantes da sala
                            sessionSet.filter { it != this }.forEach { peer ->
                                peer.send(Frame.Text(text))
                            }
                        }
                    }
                } finally {
                    sessionSet.remove(this)
                    if (sessionSet.isEmpty()) rooms.remove(roomId)
                }
            }
        }
    }.start(wait = true)
}
