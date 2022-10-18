package nl.surf.filesender.rde.client

import net.sf.scuba.util.Hex
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.Util
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/*
* This implementation is ONLY for simulating the RB call for RDE. It is not a general implementation.
* */
class AESAPDUEncoder //PassportCrypto
    (ksEnc: ByteArray, ksMac: ByteArray?) {
    private val outputStream = ByteArrayOutputStream()
    private val cipher: Cipher
    private val mac: Mac

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    private fun getIv(ksEnc: ByteArray, provider: BouncyCastleProvider): ByteArray {
        val sscIVCipher = Cipher.getInstance(IV_CIPHER, provider)
        sscIVCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(ksEnc, AES_KEY_SPEC_NAME))
        return sscIVCipher.doFinal(ssc)
    }

    @Throws(
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        IOException::class
    )
    fun write(response: ByteArray): ByteArray {
        require(response.size != 0) { "Response cannot be zero length" }
        writeDo87(response)
        writeDo99()
        writeMac()
        outputStream.write(byteArrayOf(SW1, SW2))
        return outputStream.toByteArray()
    }

    @Throws(IOException::class)
    private fun writeMac() {
        mac.update(ssc)
        val macValue = mac.doFinal(Util.pad(outputStream.toByteArray(), BLOCK_SIZE))
        outputStream.write(byteArrayOf(MAC_BLOCK_START_TAG.toByte(), MAC_LENGTH.toByte()))
        outputStream.write(macValue, 0, MAC_LENGTH)
    }

    @Throws(IOException::class)
    private fun writeDo99() {
        outputStream.write(RESPONSE_RESULT_BLOCK)
    }

    @Throws(IOException::class, BadPaddingException::class, IllegalBlockSizeException::class)
    private fun writeDo87(response: ByteArray) {
        val encodedData = getEncodedData(response)
        val sizeBlock = getEncodedDo87Size(encodedData.size)
        outputStream.write(DATA_BLOCK_START_TAG.toInt())
        outputStream.write(sizeBlock)
        outputStream.write(encodedData)
    }

    @Throws(
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        IOException::class
    )
    private fun getEncodedData(response: ByteArray): ByteArray {
        if (response.size == 0) return response
        val paddedResponse = getAlignedPlainText(response)
        return cipher.doFinal(paddedResponse)
    }

    private fun getAlignedPlainText(buffer: ByteArray): ByteArray {
        val paddedLength = getPaddedLength(buffer.size)
        return if (paddedLength == buffer.size) buffer else Util.pad(buffer, paddedLength)
    }

    fun getPaddedLength(bufferSize: Int): Int {
        return (bufferSize + BLOCK_SIZE) / BLOCK_SIZE * BLOCK_SIZE
    }

    companion object {
        const val DO87_CIPHER = "AES/CBC/NoPadding"
        const val IV_CIPHER = "AES/ECB/NoPadding"
        const val MAC_ALGO = "AESCMAC"
        const val AES_KEY_SPEC_NAME = "AES"
        private const val BLOCK_SIZE = 16 //Plain text block size cos AES and AESCMAC
        private const val SW1 = 0x90.toByte()
        private const val SW2: Byte = 0x00
        private const val DATA_BLOCK_START_TAG = 0x87.toByte()
        private const val DATA_BLOCK_LENGTH_END_TAG = 0x01.toByte()
        const val MAC_LENGTH = 0x08
        const val MAC_BLOCK_START_TAG = 0x8e
        private val RESPONSE_RESULT_BLOCK = byteArrayOf(0x99.toByte(), 0x02.toByte(), SW1, SW2)
        private val ssc = byteArrayOf(
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            2
        ) //0 when CA session started, first command is 1, first response is 2.

        //TODO make private. Only public for tests.
        fun getEncodedDo87Size(paddedDo87Length: Int): ByteArray {
            val MIN_LONG_FORM_SIZE = 0x80
            val actualLength = paddedDo87Length + 1 //Cos of the 0x01 tag
            //Short form
            if (actualLength < MIN_LONG_FORM_SIZE) return byteArrayOf(
                actualLength.toByte(),
                DATA_BLOCK_LENGTH_END_TAG
            )

            //1 or 2 byte Long form
            val lenOfLen = if (actualLength > 0xff) 2 else 1
            val result = ByteArray(lenOfLen + 2)
            result[0] = (MIN_LONG_FORM_SIZE + lenOfLen).toByte()
            var p = 1
            for (i in lenOfLen - 1 downTo 0) result[p++] =
                (actualLength ushr i * 8 and 0xff).toByte()
            result[p++] = DATA_BLOCK_LENGTH_END_TAG
            return result
        }
    }

    init {
        val provider = BouncyCastleProvider()
        cipher = Cipher.getInstance(DO87_CIPHER, provider)
        val iv = getIv(ksEnc, provider)
        println("IV: " + Hex.toHexString(iv))
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(ksEnc, AES_KEY_SPEC_NAME),
            IvParameterSpec(iv)
        )
        mac = Mac.getInstance(MAC_ALGO, provider)
        mac.init(SecretKeySpec(ksMac, MAC_ALGO))
    }
}