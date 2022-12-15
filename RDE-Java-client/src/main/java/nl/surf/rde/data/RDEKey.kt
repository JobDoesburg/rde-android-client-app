package nl.surf.rde.data

import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEKey(
    val decryptionParameters: RDEDecryptionParameters,
    val secretKey : ByteArray
) : Serializable {
    override fun toString(): String {
        return "RDEKey(decryptionParameters=$decryptionParameters)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RDEKey

        if (decryptionParameters != other.decryptionParameters) return false
        if (!secretKey.contentEquals(other.secretKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = decryptionParameters.hashCode()
        result = 31 * result + secretKey.contentHashCode()
        return result
    }
}