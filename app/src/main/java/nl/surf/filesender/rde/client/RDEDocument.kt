package nl.surf.filesender.rde.client

import net.sf.scuba.smartcards.*
import net.sf.scuba.util.Hex
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.BACKey
import org.jmrtd.PACEKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.*
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG15File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.protocol.*
import java.io.ByteArrayOutputStream
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.logging.Logger
import kotlin.experimental.and


class RDEDocument(private val documentName: String, private val bacKey: BACKey) {
    private val logger = Logger.getLogger(RDEDocument::class.java.name)

    private lateinit var passportService: PassportService

    private lateinit var paceKey: PACEKeySpec
    private lateinit var cardAccessFile: CardAccessFile
    private lateinit var securityInfo: Collection<SecurityInfo>

    private var paceInfo: PACEInfo? = null
    private var caInfo: ChipAuthenticationInfo? = null
    private var caPublicKeyInfo: ChipAuthenticationPublicKeyInfo? = null

    private var bacSucceeded = false
    private var paceSucceeded = false
    private var paSucceeded = false
    private var caSucceeded = false

    lateinit var efSOD : SODFile
    lateinit var dg1 : DG1File
    lateinit var dg2 : DG2File
    lateinit var faceImageBytes: ByteArray
    lateinit var dg14 : DG14File
    lateinit var dg15 : DG15File


    lateinit var pcdPublicKey : PublicKey
    lateinit var pcdPrivateKey : PrivateKey


    fun init(cardService: CardService) {
        passportService = PassportService(
            cardService,
            RDEDocumentConfig.TRANCEIVE_LENGTH_FOR_SECURE_MESSAGING,
            RDEDocumentConfig.MAX_BLOCK_SIZE,
            RDEDocumentConfig.SFI_ENABLED,
            true
        )
    }

    fun open() {
        if(!::passportService.isInitialized) throw IllegalStateException("init() must be called before open()")

        passportService.open()
        if (RDEDocumentConfig.USE_PACE_INSTEAD_OF_BAC && paceInfo != null) {
            readSecurityInfo()
            doPACE()
            if (paceSucceeded) {
                selectApplet()
            }
        } else {
            selectApplet()
            doBAC()
        }
    }

    private fun selectApplet() {
        passportService.sendSelectApplet(paceSucceeded)
        logger.info("Applet selected")
    }

    private fun readSecurityInfo() {
        try {
            passportService.getInputStream(
                PassportService.EF_CARD_ACCESS,
                RDEDocumentConfig.MAX_BLOCK_SIZE
            ).use { inputStream ->
                cardAccessFile = CardAccessFile(inputStream)
                logger.info("EF.CardAccess read successfully")
                securityInfo = cardAccessFile.securityInfos
                parseSecurityInfo()
            }
        } catch (e: CardServiceException) {
            throw IllegalStateException("Could not read security info", e)
        }
    }

    private fun parseSecurityInfo() {
        paceInfo = securityInfo.find { it is PACEInfo } as PACEInfo?
        caInfo = securityInfo.find { it is ChipAuthenticationInfo } as ChipAuthenticationInfo?
        caPublicKeyInfo = securityInfo.find { it is ChipAuthenticationPublicKeyInfo } as ChipAuthenticationPublicKeyInfo?

        logger.info("PACE info: $paceInfo")
        logger.info("CA info: $caInfo")
        logger.info("CA public key info: $caPublicKeyInfo")
    }

    private fun doBAC() {
        if (paceSucceeded) throw IllegalStateException("PACE already performed")
        if (bacSucceeded) throw IllegalStateException("BAC already performed")

        val bacResult = passportService.doBAC(bacKey)
        bacSucceeded = true
        logger.info("BAC succeeded: $bacResult")
    }

    private fun generatePACEKey() {
        paceKey = PACEKeySpec.createMRZKey(bacKey)
    }

    private fun getPACEKey(): PACEKeySpec {
        if (!::paceKey.isInitialized) generatePACEKey()
        return paceKey
    }

    private fun doPACE() {
        if (paceInfo == null) throw IllegalStateException("PACEInfo must be present to do PACE")
        if (bacSucceeded) throw IllegalStateException("BAC already performed")
        if (paceSucceeded) throw IllegalStateException("PACE already performed")

        paceKey = getPACEKey()
        try {
            val paceResult = passportService.doPACE(paceKey, paceInfo!!.objectIdentifier, PACEInfo.toParameterSpec(
                paceInfo!!.parameterId), paceInfo!!.parameterId)
            paceSucceeded = true
            logger.info("PACE succeeded: $paceResult")
        } catch (e: CardServiceException) {
            logger.warning("PACE failed: ${e.message}. Retrying with BAC")
            passportService.close()
            passportService.open()
            selectApplet()
            doBAC()
        }
    }

    private fun doCA(caInfo: ChipAuthenticationInfo, caPublicKeyInfo: ChipAuthenticationPublicKeyInfo) {
        // See ICAO Doc 9303 (8th edition, 2021), Part 11, Section 6.2
        if (!paceSucceeded && !bacSucceeded) throw IllegalStateException("PACE or BAC must be performed before CA")

        val caResult = passportService.doEACCA(caPublicKeyInfo.keyId, caInfo.objectIdentifier, caPublicKeyInfo.objectIdentifier, caPublicKeyInfo.subjectPublicKey)

        pcdPublicKey = caResult.pcdPublicKey
        pcdPrivateKey = caResult.pcdPrivateKey
        caSucceeded = true
        logger.info("CA succeeded: $caResult")
    }

    private fun doCustomCA(oid : String, publicKey: PublicKey) {
        // This is the trick from the RDE paper
        if (!paceSucceeded && !bacSucceeded) throw IllegalStateException("PACE or BAC must be performed before CA")

        EACCAProtocol.sendPublicKey(EACCAAPDUSender(passportService), passportService.wrapper, oid, null, publicKey);

        caSucceeded = true
        logger.info("Custom CA succeeded")
    }

    private fun readEFSOD() {
        efSOD = SODFile(passportService.getInputStream(PassportService.EF_SOD, RDEDocumentConfig.MAX_BLOCK_SIZE))
        logger.info("EF.SOD read successfully")
    }
    private fun readDG1() {
        dg1 = DG1File(passportService.getInputStream(PassportService.EF_DG1, RDEDocumentConfig.MAX_BLOCK_SIZE))
        logger.info("DG1 read successfully")
    }
    private fun readDG2() {
        dg2 = DG2File(passportService.getInputStream(PassportService.EF_DG2, RDEDocumentConfig.MAX_BLOCK_SIZE))
        logger.info("DG2 read successfully")

        getFaceImage()
    }
    private fun getFaceImage() {
        val outputStream = ByteArrayOutputStream()
        dg2.faceInfos[0].writeObject((outputStream))
        faceImageBytes = outputStream.toByteArray()
        logger.info("Face image read successfully from DG2")
    }
    private fun readDG14() {
        dg14 = DG14File(passportService.getInputStream(PassportService.EF_DG14, RDEDocumentConfig.MAX_BLOCK_SIZE))
        logger.info("DG14 read successfully")

        // after authentication, we might be able to read more data
        logger.info("Updating security info with DG14")
        securityInfo = dg14.securityInfos
        parseSecurityInfo()
    }
    private fun readDG15() {
        dg15 = DG15File(passportService.getInputStream(PassportService.EF_DG15, RDEDocumentConfig.MAX_BLOCK_SIZE))
        logger.info("DG15 read successfully")
    }

    private fun doPassiveAuth() {
        // See ICAO Doc 9303 (8th edition, 2021), Part 11, Section 5.1
        if (!::efSOD.isInitialized) throw IllegalStateException("SOD must be present to do passive authentication")

        val dataGroupIntegrityVerified = verifyDGHashes()
        val efSODIntegrityVerified = verifySODHash()
        val efSODCertificateVerified = verifySODCertificate()

        if (dataGroupIntegrityVerified && efSODIntegrityVerified && efSODCertificateVerified) {
            paSucceeded = true
            logger.info("Passive authentication succeeded")
        } else {
            throw IllegalStateException("Passive authentication failed")
        }
    }

    private fun verifyDGHashes(): Boolean {
        if (!::efSOD.isInitialized) throw IllegalStateException("efSOD must be present to verify DG hashes")

        val digest: MessageDigest = MessageDigest.getInstance(efSOD.digestAlgorithm)
        val dataHashes: Map<Int, ByteArray> = efSOD.dataGroupHashes

        if (::dg1.isInitialized){
            val computedHash: ByteArray = digest.digest(dg1.encoded)
            if (!Arrays.equals(computedHash, dataHashes[1])) throw IllegalStateException("DG1 hash mismatch")
        }
        if (::dg2.isInitialized){
            val computedHash: ByteArray = digest.digest(dg2.encoded)
            if (!Arrays.equals(computedHash, dataHashes[2])) throw IllegalStateException("DG2 hash mismatch")
        }
        if (::dg14.isInitialized){
            val computedHash: ByteArray = digest.digest(dg14.encoded)
            if (!Arrays.equals(computedHash, dataHashes[14])) throw IllegalStateException("DG14 hash mismatch")
        }
        if (::dg15.isInitialized){
            val computedHash: ByteArray = digest.digest(dg15.encoded)
            if (!Arrays.equals(computedHash, dataHashes[15])) throw IllegalStateException("DG15 hash mismatch")
        }
        logger.info("DG hashes verified successfully")
        return true
    }

    private fun verifySODHash(): Boolean {
        val s = Signature.getInstance(efSOD.digestEncryptionAlgorithm)
            .apply {
                initVerify(efSOD.docSigningCertificate)
                update(efSOD.eContent)
            }
        val verified = s.verify(efSOD.encryptedDigest)
        if (!verified) throw IllegalStateException("SOD hash verification failed")
        logger.info("SOD hash verified successfully")
        return verified
    }

    private fun verifySODCertificate(): Boolean {
        // TODO implement

        return true
    }

    private fun doRBCall(dgId: Int, length: Int): ByteArray {
        val rbCommand = readBinaryCommand(dgId, length)
        val rbCommandWrapped = passportService.wrapper.wrap(rbCommand)
        logger.info("Sending RB command for DG $dgId with length $length: $rbCommand (wrapped: ${rbCommandWrapped})")

        val rbResponse = passportService.transmit(rbCommandWrapped)

        val unwrappedResponse = passportService.wrapper.unwrap(rbResponse)
        logger.info("Received RB response: $rbResponse (unwrapped: $unwrappedResponse)")

        val rbResponseData = getResponseData(unwrappedResponse,false)
        if (rbResponseData.size != length) throw IllegalStateException("RB response data length mismatch")
        return rbResponseData
    }

    fun enroll() : RDEEnrollmentParameters {
        if (!paceSucceeded && !bacSucceeded) throw IllegalStateException("PACE or BAC must be performed before CA")

        readEFSOD()
        readDG1()
//        readDG2()
        readDG14()
        readDG15()

        try {
            doPassiveAuth()
        } catch (e: Exception) {
            logger.warning("Passive authentication failed, trying active authentication: $e")
        }

        doCA(caInfo!!, caPublicKeyInfo!!)

        val rbResponseData = doRBCall(RDEDocumentConfig.DG_ID_FOR_RDE, RDEDocumentConfig.MAX_BLOCK_SIZE)

        passportService.close()

        return RDEEnrollmentParameters(RDEDocumentConfig.MAX_BLOCK_SIZE, RDEDocumentConfig.DG_ID_FOR_RDE, Hex.toHexString(rbResponseData), caInfo!!.objectIdentifier, Hex.toHexString(caPublicKeyInfo!!.subjectPublicKey.encoded), documentName)
    }

    fun decrypt(parameters: RDEDecryptionParameters) : ByteArray {
        if (!paceSucceeded && !bacSucceeded) throw IllegalStateException("PACE or BAC must be performed before CA")

        readEFSOD()
        readDG1()
//        readDG2()
        readDG14()
        readDG15()

        try {
            doPassiveAuth()
        } catch (e: Exception) {
            logger.warning("Passive authentication failed, trying active authentication: $e")
        }

        val publicKey = decodePublicKey(parameters.oid, Hex.hexStringToBytes(parameters.publicKey))
        doCustomCA(parameters.oid, publicKey)

        val protectedCommand = CommandAPDU(Hex.hexStringToBytes(parameters.protectedCommand))
        logger.info("Sending protected command: $protectedCommand")

        val response = passportService.transmit(protectedCommand)
        logger.info("Received response: $response")

        return getDecryptionKeyFromAPDUResponse(response.bytes)
    }

    companion object {
        fun readBinaryCommand(fid :  Int, length : Int) : CommandAPDU {
            return getReadBinaryAPDU(fid, 0, length, RDEDocumentConfig.SFI_ENABLED, false)
        }
        private fun getReadBinaryAPDU(
            sfi: Int,
            offset: Int,
            le: Int,
            isSFIEnabled: Boolean,
            isTLVEncodedOffsetNeeded: Boolean
        ): CommandAPDU {
            var le = le
            val offsetMSB = (offset and 0xFF00 shr 8).toByte()
            val offsetLSB = (offset and 0xFF).toByte()
            return if (isTLVEncodedOffsetNeeded) {
                // In the case of long read 2 or 3 bytes less of the actual data will be returned,
                // because a tag and length will be sent along, here we need to account for this.
                if (le < 128) {
                    le += 2
                } else if (le < 256) {
                    le += 3
                }
                if (le > 256) {
                    le = 256
                }
                val data = byteArrayOf(0x54, 0x02, offsetMSB, offsetLSB)
                CommandAPDU(
                    ISO7816.CLA_ISO7816.toInt(),
                    ISO7816.INS_READ_BINARY2.toInt(), 0, 0, data, le
                )
            } else if (isSFIEnabled) {
                CommandAPDU(
                    ISO7816.CLA_ISO7816.toInt(),
                    ISO7816.INS_READ_BINARY.toInt(),
                    sfi.toByte().toInt(),
                    offsetLSB.toInt(),
                    le
                )
            } else {
                CommandAPDU(ISO7816.CLA_ISO7816.toInt(), ISO7816.INS_READ_BINARY.toInt(), offsetMSB.toInt(), offsetLSB.toInt(), le)
            }
        }

        fun getResponseData(
            responseAPDU: ResponseAPDU,
            isTLVEncodedOffsetNeeded: Boolean
        ): ByteArray {
            var responseData = responseAPDU.data
            if (!isTLVEncodedOffsetNeeded) {
                return responseData
            }

            /*
         * Strip the response off the tag 0x53 and the length field.
         * FIXME: Use TLVUtil.tlvEncode(...) here. -- MO
         */
            val data = responseData
            var index = 0
            if (data[index++] != 0x53.toByte()) { // FIXME: Constant for 0x53.
                throw CardServiceException("Malformed read binary long response data")
            }
            if ((data[index] and 0x80.toByte()) == 0x80.toByte()) {
                index += data[index] and 0xF
            }
            index++
            responseData = ByteArray(data.size - index)
            System.arraycopy(data, index, responseData, 0, responseData.size)
            return responseData
        }


        fun encryptCommand(command : CommandAPDU,  oid: String, sharedSecret : ByteArray, maxTranceiveLength: Int) : CommandAPDU {
            val wrapper = EACCAProtocol.restartSecureMessaging(oid, sharedSecret, maxTranceiveLength, true)
            return wrapper.wrap(command)
        }
        fun getDecryptionKeyFromAPDUResponse(response: ByteArray): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(response)
        }
        fun agreementAlgFromCAOID(oid: String) : String {
            val agreementAlg = ChipAuthenticationInfo.toKeyAgreementAlgorithm(oid)
                ?: throw IllegalArgumentException("Unknown agreement algorithm")
            require("ECDH" == agreementAlg || "DH" == agreementAlg) { "Unsupported agreement algorithm, expected ECDH or DH, found $agreementAlg" }
            return agreementAlg
        }
        fun decodePublicKey(oid: String, keyBytes: ByteArray): PublicKey {
            val agreementAlg = agreementAlgFromCAOID(oid)
            val keyFactory = KeyFactory.getInstance(agreementAlg, BouncyCastleProvider())
            return keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
        }

    }



}