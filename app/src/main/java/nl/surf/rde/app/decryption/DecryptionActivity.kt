package nl.surf.rde.app.decryption

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.surf.rde.app.MainActivity
import nl.surf.rde.app.common.ScanQRActivity
import nl.surf.rde.app.decryption.handshake.DecryptionHandshakeProtocol
import nl.surf.rde.app.common.DocumentMRZData
import nl.surf.rde.app.common.ReadMRZActivity
import nl.surf.rde.data.RDEDecryptionParameters


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
    private lateinit var mrzData: DocumentMRZData
    private lateinit var retrievedKey: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchQRScanner()
    }

    private fun launchQRScanner() {
        val intent = Intent(this, ScanQRActivity::class.java)
        startActivityForResult(intent, LAUNCH_QR_SCANNER)
    }

    private fun launchMRZInput() {
        val intent = Intent(this, ReadMRZActivity::class.java)
        Toast.makeText(this, "This document is encrypted with ${decryptionParams.documentName}", Toast.LENGTH_LONG).show()
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
                mrzData = data?.extras!!["result"] as DocumentMRZData
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
                try {
                    sendRetrievedKey()
                } catch (e: Exception) {
                    Log.e("DecryptionActivity", "Error while sending key: $e")
                    Toast.makeText(this, "Error while sending key: $e", Toast.LENGTH_LONG).show()
                    finish()
                }
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