package nl.surf.filesender.rde.client.activities.decryption

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.surf.filesender.rde.client.activities.MainActivity
import nl.surf.filesender.rde.client.activities.general.ScanQRActivity
import nl.surf.filesender.rde.client.handshake.DecryptionHandshakeProtocol
import nl.surf.filesender.rde.data.RDEDecryptionParameters
import nl.surf.filesender.rde.client.RDEDocumentMRZData


class DecryptionActivity : AppCompatActivity() {
    private val client = HttpClient(OkHttp) { // We need to use OkHttp because of DNS + IPv6 issues with CIO engine
        install(WebSockets) {
            pingInterval = 25_000
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    private lateinit var socket : DefaultWebSocketSession

    private lateinit var socketUrl : String
    private lateinit var handshake : DecryptionHandshakeProtocol
    private lateinit var decryptionParams: RDEDecryptionParameters
    private lateinit var mrzData: RDEDocumentMRZData
    private lateinit var retrievedKey: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchQRScanner()
    }

    fun launchQRScanner() {
        val intent = Intent(this, ScanQRActivity::class.java)
        startActivityForResult(intent, LAUNCH_QR_SCANNER)
    }

    private fun launchMRZInput() {
        val intent = Intent(this, DecryptionReadMRZActivity::class.java)
//        intent.putExtra("documentName", decryptionParams.documentName) // TODO add document name to decryption params
        startActivityForResult(intent, LAUNCH_MRZ_INPUT)
    }

    private fun launchReadNFC() {
        val intent = Intent(this, DecryptionReadNFCActivity::class.java)
        intent.putExtra("mrzData", mrzData)
        intent.putExtra("decryptionParams", decryptionParams)
        startActivityForResult(intent, LAUNCH_READ_NFC)
    }

    private fun startDecryption() {
        runBlocking {
            socket = client.webSocketSession(urlString = socketUrl)
            handshake = DecryptionHandshakeProtocol(socket)
            handshake.generateKeyPair()
            val receivedData = handshake.performHandshake()
            decryptionParams = Json.decodeFromString(receivedData)
            Log.d("DecryptionActivity", "Received decryption parameters: $decryptionParams")
            launchMRZInput()
        }
    }

    private fun sendRetrievedKey() {
        runBlocking {
            handshake.sendData(retrievedKey)
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LAUNCH_QR_SCANNER) {
            if (resultCode == RESULT_OK) {
                socketUrl = data?.getStringExtra("result")!!
                Log.d("DecryptionActivity", "Received socket url: $socketUrl")
                startDecryption()
            } else if (resultCode == RESULT_CANCELED) {
                finish()
            }
        }
        if (requestCode == LAUNCH_MRZ_INPUT) {
            if (resultCode == RESULT_OK) {
                mrzData = data?.extras!!["result"] as RDEDocumentMRZData
                Log.d("DecryptionActivity", "Received MRZ data: $mrzData")
                launchReadNFC()
            } else if (resultCode == RESULT_CANCELED) {
                launchQRScanner()
            }
        }
        if (requestCode == LAUNCH_READ_NFC) {
            if (resultCode == RESULT_OK) {
                retrievedKey = data?.getByteArrayExtra("result")!!
                Log.d("DecryptionActivity", "Received key: $retrievedKey")
                sendRetrievedKey()
            } else if (resultCode == RESULT_CANCELED) {
                launchMRZInput()
            }
        }
    }

    override fun finish() {
        client.close()
        super.finish()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        const val LAUNCH_QR_SCANNER = 1
        const val LAUNCH_MRZ_INPUT = 2
        const val LAUNCH_READ_NFC = 3
    }
}