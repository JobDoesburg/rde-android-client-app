package nl.surf.filesender.rde.client

import kotlinx.serialization.Serializable

@Serializable
data class RDEDecryptionParameters (
    val oid: String,
    val publicKey: ByteArray,
    val protectedCommand: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RDEDecryptionParameters

        if (oid != other.oid) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!protectedCommand.contentEquals(other.protectedCommand)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oid.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + protectedCommand.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "RDEDecryptionParameters(oid='$oid', publicKey=${publicKey.contentToString()}, protectedCommand=${protectedCommand.contentToString()})"
    }
}
