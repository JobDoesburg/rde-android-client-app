package nl.surf.filesender.rde.client.activities.decryption

import android.os.Bundle
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import nl.surf.filesender.rde.client.activities.general.ResultActivity
import nl.surf.filesender.rde.client.handshake.DecryptionHandshakeProtocol

class DecryptionResultActivity : ResultActivity() {
    private lateinit var websocketSessionIV: ByteArray
    private lateinit var websocketSharedSecret: ByteArray
    private lateinit var websocketUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        websocketUrl = intent.getStringExtra("decryption_socket_url")!!
        websocketSharedSecret = intent.getByteArrayExtra("decryption_handshake_shared_secret")!!
        websocketSessionIV = intent.getByteArrayExtra("decryption_handshake_iv")!!
    }

    private suspend fun sendResult(url : String, data: String) {
        val client = HttpClient(CIO) {
            install(WebSockets) {
                pingInterval = 20_000
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }
        client.webSocket(urlString = url) {
            val handshake = DecryptionHandshakeProtocol(this, websocketSessionIV, websocketSharedSecret)
            handshake.sendData(data.toByteArray())
        }
        client.close()
    }

    override fun onDoneButtonClick() {
        val url = websocketUrl
        val data = resultData
        runBlocking {
            sendResult(url, data)
        }
        super.onDoneButtonClick()
    }

}