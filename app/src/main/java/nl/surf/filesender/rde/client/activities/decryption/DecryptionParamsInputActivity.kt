package nl.surf.filesender.rde.client.activities.decryption

import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import nl.surf.filesender.rde.client.activities.general.ParamsInputActivity
import nl.surf.filesender.rde.client.handshake.DecryptionHandshakeProtocol

class DecryptionParamsInputActivity : ParamsInputActivity() {
    private lateinit var websocketUrl: String
    private lateinit var handshake: DecryptionHandshakeProtocol
    override var nextActivity: Class<*> = DecryptionReadMRZActivity::class.java

    private suspend fun getParamsFromServer(url : String): String? {
        var params : String? = null
        val client = HttpClient(CIO) {
            install(WebSockets) {
                pingInterval = 600_000
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }
        client.webSocket(urlString = url) {
            handshake = DecryptionHandshakeProtocol(this)
            handshake.generateKeyPair()
            params = handshake.performHandshake()
            Log.d("DecryptionParamsInput", "Received params: $params")
        }
        return params
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        websocketUrl = intent.getStringExtra("decryption_socket_url")!!
        Log.d("DecryptionParamsInput", "websocketUrl: $websocketUrl")
        runBlocking {
            val params = getParamsFromServer(websocketUrl)
            if (params != null) {
                inputDataField.setText(params)
            }
        }
    }

    override fun getNextActivityIntent(): Intent {
        intent = super.getNextActivityIntent()
        intent.putExtra("decryption_socket_url", websocketUrl)
        intent.putExtra("decryption_handshake_shared_secret", handshake.sharedSecret)
        intent.putExtra("decryption_handshake_iv", handshake.iv)
        return intent
    }


}