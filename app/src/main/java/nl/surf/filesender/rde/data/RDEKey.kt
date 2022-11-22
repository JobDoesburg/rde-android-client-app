package nl.surf.filesender.rde.data

import net.sf.scuba.util.Hex
import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEKey(
    val encryptionKey : ByteArray,
    val decryptionParameters: RDEDecryptionParameters
) : Serializable {
    override fun toString(): String {
        return "RDEKey(encryptionKey='${Hex.toHexString(encryptionKey)}', decryptionParameters=$decryptionParameters)"
    }
}