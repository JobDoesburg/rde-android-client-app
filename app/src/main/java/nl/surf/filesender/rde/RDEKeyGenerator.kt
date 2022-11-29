package nl.surf.filesender.rde

import net.sf.scuba.util.Hex
import nl.surf.filesender.rde.data.RDEDecryptionParameters
import nl.surf.filesender.rde.data.RDEEnrollmentParameters
import nl.surf.filesender.rde.data.RDEKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.PassportService
import org.jmrtd.Util
import org.jmrtd.lds.ChipAuthenticationInfo
import org.jmrtd.protocol.EACCAProtocol
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.interfaces.DHPublicKey

/**
 * Generator for RDE keys.
 */
class RDEKeyGenerator(var enrollmentParams: RDEEnrollmentParameters) {
    private val caOID : String = enrollmentParams.caOID
    private val agreementAlg: String = RDEDocument.agreementAlgFromCAOID(caOID)
    private val piccPublicKey : PublicKey =
        RDEDocument.decodePublicKey(caOID, Hex.hexStringToBytes(enrollmentParams.piccPublicKey))
    private val params : AlgorithmParameterSpec = paramsFromPublicKey(agreementAlg, piccPublicKey)
    private val cipherAlg : String = ChipAuthenticationInfo.toCipherAlgorithm(caOID)
    private val keyLength : Int = ChipAuthenticationInfo.toKeyLength(caOID)

    /**
     * Generate a new RDE key.
     */
    fun generateKey() : RDEKey {
        val pcdKeyPair = generateKeyPair(agreementAlg, params)
        val pcdPublicKey = pcdKeyPair.public
        val pcdPrivateKey = pcdKeyPair.private
        val sharedSecret = EACCAProtocol.computeSharedSecret(agreementAlg, piccPublicKey, pcdPrivateKey)

        val encryptionKey = deriveEncryptionKey(sharedSecret)
        val protectedCommand = generateProtectedCommand(sharedSecret)
        val decryptionParams = RDEDecryptionParameters(enrollmentParams.documentName, caOID, Hex.toHexString(pcdPublicKey.encoded), Hex.toHexString(protectedCommand))
        return RDEKey(encryptionKey, decryptionParams)
    }

    /**
     * Derives the encryption key from the given shared secret.
     * @param sharedSecret the shared secret
     */
    private fun deriveEncryptionKey(sharedSecret : ByteArray) : ByteArray {
        val ksEnc = Util.deriveKey(sharedSecret, cipherAlg, keyLength, Util.ENC_MODE)
        val ksMac = Util.deriveKey(sharedSecret, cipherAlg, keyLength, Util.MAC_MODE)
        val emulatedResponse = AESAPDUEncoder(ksEnc.encoded, ksMac.encoded).write(Hex.hexStringToBytes(enrollmentParams.rdeDGContent))
        return RDEDocument.getDecryptionKeyFromAPDUResponse(emulatedResponse)
    }

    /**
     * Generates a protected command for the given RDE document, required to retrieve the decryption key.
     * @param sharedSecret the shared secret
     */
    private fun generateProtectedCommand(sharedSecret : ByteArray) : ByteArray {
        val rbCommand = RDEDocument.readBinaryCommand(enrollmentParams.rdeDGId, enrollmentParams.rdeRBLength)
        val protectedCommand = RDEDocument.encryptCommand(
            rbCommand,
            caOID,
            sharedSecret,
            PassportService.NORMAL_MAX_TRANCEIVE_LENGTH
        )
        return protectedCommand.bytes
    }

    companion object {
        private fun paramsFromPublicKey(agreementAlg: String, publicKey: PublicKey) : AlgorithmParameterSpec {
            if ("DH" == agreementAlg) {
                val passportDHPublicKey = publicKey as DHPublicKey
                return passportDHPublicKey.params
            } else if ("ECDH" == agreementAlg) {
                val passportECPublicKey = publicKey as ECPublicKey
                return passportECPublicKey.params
            }
            throw IllegalArgumentException("Unsupported agreement algorithm, expected ECDH or DH, found $agreementAlg")
        }

        fun generateKeyPair(agreementAlg: String, params: AlgorithmParameterSpec): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance(agreementAlg, BouncyCastleProvider())
            keyPairGenerator.initialize(params)
            return keyPairGenerator.generateKeyPair()
        }
    }


}