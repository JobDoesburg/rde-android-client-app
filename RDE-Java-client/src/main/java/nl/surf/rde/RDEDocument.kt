package nl.surf.rde

import net.sf.scuba.smartcards.*
import net.sf.scuba.util.Hex
import nl.surf.rde.data.RDEDecryptionParameters
import nl.surf.rde.data.RDEEnrollmentParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.BACKey
import org.jmrtd.PACEKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.*
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.protocol.EACCAAPDUSender
import org.jmrtd.protocol.EACCAProtocol
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.logging.Logger
import kotlin.experimental.and


class RDEDocument(private val bacKey: BACKey) { // TODO add CAN support
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
    lateinit var dg14 : DG14File
    lateinit var dg14Bytes: ByteArray // Due to some bug we need to store the bytes of DG14
    // separately, as for some models of passports, the computed encoded DG14 contents are different
    // from the actual contents of the file on the passport (when reading), making signature
    // verification fail.

    private lateinit var pcdPublicKey : PublicKey
    private lateinit var pcdPrivateKey : PrivateKey

    fun init(cardService: CardService, maxTranceiveLength: Int = PassportService.NORMAL_MAX_TRANCEIVE_LENGTH, maxBlockSize: Int = PassportService.DEFAULT_MAX_BLOCKSIZE) {
        if (maxBlockSize > PassportService.DEFAULT_MAX_BLOCKSIZE) {
            throw IllegalArgumentException("Max block size cannot be larger than ${PassportService.DEFAULT_MAX_BLOCKSIZE}")
        }
        if (maxTranceiveLength > PassportService.NORMAL_MAX_TRANCEIVE_LENGTH) {
            throw IllegalArgumentException("Max tranceive length cannot be larger than ${PassportService.NORMAL_MAX_TRANCEIVE_LENGTH}")
        }

        passportService = PassportService(
            cardService,
            maxTranceiveLength,
            maxBlockSize,
            true,
            true
        ) // TODO maybe dont init from a CardService but from a Tag, and do this in the constructor
    }

    fun open() {
        if(!::passportService.isInitialized) throw IllegalStateException("init() must be called before open()")

        passportService.open()
        readSecurityInfo()
        if (paceInfo != null) {
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
                passportService.maxTranceiveLength
            ).use { inputStream ->
                cardAccessFile = CardAccessFile(inputStream)
                logger.info("EF.CardAccess read successfully")
                securityInfo = cardAccessFile.securityInfos
                parseSecurityInfo()
            }
        } catch (e: Exception) {
            logger.warning("Could not read security info")
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
        efSOD = SODFile(passportService.getInputStream(PassportService.EF_SOD,
            passportService.maxReadBinaryLength
        ))
        logger.info("EF.SOD read successfully")
    }
    private fun readDG1() {
        dg1 = DG1File(passportService.getInputStream(PassportService.EF_DG1,
            passportService.maxReadBinaryLength
        ))
        logger.info("DG1 read successfully")
    }
    private fun readDG2() {
        dg2 = DG2File(passportService.getInputStream(PassportService.EF_DG2,
            passportService.maxReadBinaryLength
        ))
        logger.info("DG2 read successfully")
    }

    private fun readDG14() {
        // dg14 = DG14File(passportService.getInputStream(PassportService.EF_DG14,
        //    passportService.maxReadBinaryLength
        // ))
        //
        // We can't do it this way, because due to some bug, retrieving the encoded contents
        // of the file will give back different results (parts of the ASN will be ordered
        // differently), which will make the signature verification fail.
        // So we have to do it the hard way, by reading the file manually ourselves

        dg14Bytes = readDGwithRB(14)
        dg14 = DG14File(ByteArrayInputStream(dg14Bytes))

        logger.info("DG14 read successfully")

        // after authentication, we might be able to read more data
        logger.info("Updating security info with DG14")
        securityInfo = dg14.securityInfos
        parseSecurityInfo()
    }

    private fun doPassiveAuth() {
        // See ICAO Doc 9303 (8th edition, 2021), Part 11, Section 5.1
        if (!::efSOD.isInitialized) throw IllegalStateException("SOD must be present to do passive authentication")

        val dataGroupIntegrityVerified = verifyDGHashes()
        val efSODIntegrityVerified = verifySODHash()
        val efSODCertificateVerified =  true // TODO: verify SOD certificate. This requires a trusted list of root certificates, which we don't maintain in this app

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
            val computedHash: ByteArray = digest.digest(dg14Bytes) // We can't use dg14.encoded, because on certain models of passports, it will be different from the original file
            if (!Arrays.equals(computedHash, dataHashes[14])) throw IllegalStateException("DG14 hash mismatch")
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

    private fun doRBCall(dgId: Int, length: Int): ByteArray {
        val rbCommand = readBinaryCommand(dgId, length)
        val rbCommandWrapped = passportService.wrapper.wrap(rbCommand)
        logger.info("Sending RB command for DG $dgId with length $length: $rbCommand (wrapped: ${rbCommandWrapped})")

        val rbResponse = passportService.transmit(rbCommandWrapped)

        val unwrappedResponse = passportService.wrapper.unwrap(rbResponse)
        logger.info("Received RB response: $rbResponse (unwrapped: $unwrappedResponse)")

        val rbResponseData = getResponseData(unwrappedResponse,false)
        if (rbResponseData.size != length) throw IllegalStateException("RB response data length mismatch. Expected $length, got ${rbResponseData.size}")
        return rbResponseData
    }

    private fun readDGwithRB(dgId: Int): ByteArray {
        val stream = passportService.getInputStream(dgIDToEFDGID(dgId), passportService.maxReadBinaryLength)
        return readAllBytes(stream)!!
    }

    fun enroll(documentName : String, rdeDGId : Int, rdeRBLength : Int, withSecurityData: Boolean = true, withMRZData: Boolean = true, withFaceImage: Boolean = false) : RDEEnrollmentParameters {
        if (!paceSucceeded && !bacSucceeded) throw IllegalStateException("PACE or BAC must be performed before CA")
        if (rdeDGId > 15 || rdeDGId < 1) throw IllegalArgumentException("rdeDGId must be between 1 and 15 (and must be a data group that is present on the passport that can be read without terminal authentication)")
        if (rdeRBLength > passportService.maxReadBinaryLength || rdeRBLength < 1) throw IllegalArgumentException("rdeRBLength must be between 1 and ${passportService.maxReadBinaryLength}")

        readEFSOD() // needed for passive authentication, so always do this regardless of whether we're doing enrollment withSecurityData
        readDG14() // required to get security info for the CA session

        if (withMRZData) {
            readDG1()
        }
        if (withFaceImage) {
            readDG2()
        }

        try {
            if (withSecurityData) {
                doPassiveAuth()
            }
        } catch (e: Exception) {
            logger.warning("Passive authentication failed: ${e.message}")
        }

        doCA(caInfo!!, caPublicKeyInfo!!)

        // TODO technically, we don't need to do CA at all, but let's do it anyway for now so we are certain that CA is supported and our DG contents dont change after CA (which might be the case for certain data groups)

        val rbResponse = doRBCall(rdeDGId, rdeRBLength)
        val rdeDGContent = if (withSecurityData) { // If we include security data, we include the entire DG, because otherwise the dg hashes won't verify. Otherwise we just include the RB response that is shorter, because we don't need the full DG for simple RDE
            Hex.toHexString(readDGwithRB(rdeDGId))
        } else {
            Hex.toHexString(rbResponse)
        }

        val caOid = caInfo!!.objectIdentifier
        val piccPublicKeyData = Hex.toHexString(caPublicKeyInfo!!.subjectPublicKey.encoded)
        val securityData = if (withSecurityData) Hex.toHexString(efSOD.encoded).replace("\n", "") else null
        val mrzData = if (withMRZData) Hex.toHexString(dg1.encoded).replace("\n", "") else null
        val faceImageData = if (withFaceImage) Hex.toHexString(dg2.encoded).replace("\n", "") else null


        return RDEEnrollmentParameters(
            documentName,
            caOid,
            piccPublicKeyData,
            rdeDGId,
            rdeRBLength,
            rdeDGContent,
            securityData,
            mrzData,
            faceImageData,
        )
    }

    fun decrypt(parameters: RDEDecryptionParameters) : ByteArray {
        if (!paceSucceeded && !bacSucceeded) throw IllegalStateException("PACE or BAC must be performed before CA")

        readEFSOD()

        try {
            doPassiveAuth()
        } catch (e: Exception) {
            logger.warning("Passive authentication failed, trying active authentication: $e")
        }

        val publicKey = decodePublicKey(parameters.caOID, Hex.hexStringToBytes(parameters.pcdPublicKey))
        doCustomCA(parameters.caOID, publicKey)

        val protectedCommand = CommandAPDU(Hex.hexStringToBytes(parameters.protectedCommand))
        logger.info("Sending protected command: $protectedCommand")

        val response = passportService.transmit(protectedCommand)
        logger.info("Received response: $response")

        return getDecryptionKeyFromAPDUResponse(response.bytes)
    }

    fun close() {
        passportService.close()
    }

    companion object {
        fun readBinaryCommand(fid :  Int, length : Int) : CommandAPDU {
            return getReadBinaryAPDU(fid, 0, length, true, false)
        }

        // TODO this is copied from jmrtd, should be refactored to be made easier, or attribute to jmrtd
        private fun getReadBinaryAPDU(
            sfi: Int,
            offset: Int,
            length: Int,
            isSFIEnabled: Boolean,
            isTLVEncodedOffsetNeeded: Boolean
        ): CommandAPDU {
            var le = length
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
                val sfiByte = 0x80 or (sfi and 0xFF)
                return CommandAPDU(
                    ISO7816.CLA_ISO7816.toInt(),
                    ISO7816.INS_READ_BINARY.toInt(),
                    sfiByte.toByte().toInt(),
                    0,
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

        private fun readAllBytes(inputStream: InputStream): ByteArray? {
            val buffer = ByteArray(4096) // TODO this should be a lot bigger (check ICAO spec)
            ByteArrayOutputStream(4096).use { result ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    result.write(buffer, 0, bytesRead)
                }
                result.flush()
                return result.toByteArray()
            }
        }

        fun dgIDToEFDGID(dgId: Int) : Short {
            return when (dgId) {
                1 -> PassportService.EF_DG1;
                2 -> PassportService.EF_DG2;
                3 -> PassportService.EF_DG3;
                4 -> PassportService.EF_DG4;
                5 -> PassportService.EF_DG5;
                6 -> PassportService.EF_DG6;
                7 -> PassportService.EF_DG7;
                8 -> PassportService.EF_DG8;
                9 -> PassportService.EF_DG9;
                10 -> PassportService.EF_DG10;
                11 -> PassportService.EF_DG11;
                12 -> PassportService.EF_DG12;
                13 -> PassportService.EF_DG13;
                14 -> PassportService.EF_DG14;
                15 -> PassportService.EF_DG15;
                16 -> PassportService.EF_DG16;
                else -> {
                    throw IllegalArgumentException("Unknown DG ID: $dgId")
                }
            }
        }
    }
}