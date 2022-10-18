package nl.surf.filesender.rde.client

import kotlinx.serialization.Serializable

@Serializable
data class RDEDecryptionParameters (
    val oid: String,
    val publicKey: String,
    val protectedCommand: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RDEDecryptionParameters

        if (oid != other.oid) return false
        if (publicKey != other.publicKey) return false
        if (protectedCommand != other.protectedCommand) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oid.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + protectedCommand.hashCode()
        return result
    }

    override fun toString(): String {
        return "RDEDecryptionParameters(oid='$oid', publicKey='$publicKey', protectedCommand='$protectedCommand')"
    }
}
