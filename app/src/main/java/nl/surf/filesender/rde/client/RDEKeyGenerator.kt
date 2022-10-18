package nl.surf.filesender.rde.client

import net.sf.scuba.util.Hex
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.Util
import org.jmrtd.lds.ChipAuthenticationInfo
import org.jmrtd.protocol.EACCAProtocol
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.interfaces.DHPublicKey

class RDEKeyGenerator(var enrollmentParams: RDEEnrollmentParameters) {
    private val oid : String = enrollmentParams.caOid
    private val agreementAlg: String = RDEDocument.agreementAlgFromCAOID(oid)
    private val piccPublicKey : PublicKey = RDEDocument.decodePublicKey(oid, Hex.hexStringToBytes(enrollmentParams.piccPublicKey))
    private val params : AlgorithmParameterSpec = paramsFromPublicKey(agreementAlg, piccPublicKey)
    private val cipherAlg : String = ChipAuthenticationInfo.toCipherAlgorithm(oid)
    private val keyLength : Int = ChipAuthenticationInfo.toKeyLength(oid)

    fun generateKey() : RDEKey {
        val pcdKeyPair = generateKeyPair(agreementAlg, params)
        val pcdPublicKey = pcdKeyPair.public
        val pcdPrivateKey = pcdKeyPair.private
        val sharedSecret = EACCAProtocol.computeSharedSecret(agreementAlg, piccPublicKey, pcdPrivateKey)

        val encryptionKey = deriveEncryptionKey(sharedSecret)
        val protectedCommand = generateProtectedCommand(sharedSecret)
        val decryptionParams = RDEDecryptionParameters(oid, Hex.toHexString(pcdPublicKey.encoded), Hex.toHexString(protectedCommand))
        return RDEKey(encryptionKey, decryptionParams)
    }

    private fun deriveEncryptionKey(sharedSecret : ByteArray) : ByteArray {
        val ksEnc = Util.deriveKey(sharedSecret, cipherAlg, keyLength, Util.ENC_MODE)
        val ksMac = Util.deriveKey(sharedSecret, cipherAlg, keyLength, Util.MAC_MODE)
        val emulatedResponse = AESAPDUEncoder(ksEnc.encoded, ksMac.encoded).write(Hex.hexStringToBytes(enrollmentParams.Fcont))
        return RDEDocument.getDecryptionKeyFromAPDUResponse(emulatedResponse)
    }

    private fun generateProtectedCommand(sharedSecret : ByteArray) : ByteArray {
        val rbCommand = RDEDocument.readBinaryCommand(enrollmentParams.Fid, enrollmentParams.n)
        val protectedCommand = RDEDocument.encryptCommand(rbCommand, oid, sharedSecret, RDEDocumentConfig.TRANCEIVE_LENGTH_FOR_SECURE_MESSAGING)
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