package nl.surf.filesender.rde.client.handshake

import com.nimbusds.jose.jwk.*
import io.ktor.websocket.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.sf.scuba.util.Hex
import nl.surf.filesender.rde.RDEDocument
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class DecryptionHandshakeProtocol(private val socket: DefaultWebSocketSession) {
    companion object {
        const val CIPHER_ALG = "AES"
        const val CIPHER_ALG_MODE = "AES/CBC/PKCS5Padding"
        const val CIPHER_ALG_KEY_SIZE = 256
        const val KEY_AGREEMENT = "ECDH"
        const val KEY_AGREEMENT_TYPE = "EC"
        const val KEY_AGREEMENT_PARAMS = "secp384r1"
        val KEY_AGREEMENT_CURVE: Curve = Curve.P_384

    }

    lateinit var iv: ByteArray
    lateinit var sharedSecret: ByteArray
    private lateinit var browserKey: ECPublicKey
    private val logger = Logger.getLogger(RDEDocument::class.java.name)
    private lateinit var appKeyPair: KeyPair

    fun generateKeyPair () {
        val ecParamSpec = ECGenParameterSpec(KEY_AGREEMENT_PARAMS)
        val kpg = KeyPairGenerator.getInstance(KEY_AGREEMENT_TYPE)
        kpg.initialize(ecParamSpec)
        appKeyPair = kpg.genKeyPair()
    }

    private fun encodeAsJWK(): String{
        return ECKey.Builder(KEY_AGREEMENT_CURVE, appKeyPair.public as ECPublicKey)
            .build().toJSONString()
    }

    private suspend fun sendAppKey() {
        val encodedPublicKey = encodeAsJWK()
        logger.info("Keypair: $encodedPublicKey")
        socket.send(encodedPublicKey)
    }

    constructor(socket: DefaultWebSocketSession, iv: ByteArray, sharedSecret: ByteArray) : this(socket) {
        this.iv = iv
        this.sharedSecret = sharedSecret
    }

    @Serializable
    data class BrowserResponse(
        val key: JsonElement,
        val iv: String,
    )

    private fun receiveBrowserKey(data: String) {
        val browserResponse = Json.decodeFromString<BrowserResponse>(data)
        logger.info("IV ${browserResponse.iv}")
        logger.info("Key ${browserResponse.key}")
        iv = Hex.hexStringToBytes(browserResponse.iv)
        browserKey = JWK.parse(browserResponse.key.toString()).toECKey().toECPublicKey()
        deriveSharedSecret()
    }

    private fun deriveSharedSecret() {
        val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT)
        keyAgreement.init(appKeyPair.private)
        keyAgreement.doPhase(browserKey, true)
        sharedSecret = keyAgreement.generateSecret().take(CIPHER_ALG_KEY_SIZE/8).toByteArray()
        logger.info("Shared secret: ${Hex.toHexString(sharedSecret)}")
    }

    suspend fun performHandshake(): String {
        sendAppKey()
        val response = socket.incoming.receive()
        receiveBrowserKey(String(response.data))

        val encryptedParams = socket.incoming.receive()
        logger.info("encrypted params: ${Hex.toHexString(encryptedParams.data)}")

        val cipher = Cipher.getInstance(CIPHER_ALG_MODE)
        val keySpec = SecretKeySpec(sharedSecret, CIPHER_ALG)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(encryptedParams.data)
        val result = String(decrypted)
        logger.info("Result $result")
        return result
    }

    suspend fun sendData(data: ByteArray) {
        val cipher = Cipher.getInstance(CIPHER_ALG_MODE)
        val keySpec = SecretKeySpec(sharedSecret, CIPHER_ALG)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(data)
        socket.send(encrypted)
        logger.info("Sent ${Hex.toHexString(encrypted)}")
    }

}