package nl.surf.filesender.rde.data

import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEDecryptionParameters (
    val oid: String,
    val publicKey: String,
    val protectedCommand: String,
) : Serializable {
    override fun toString(): String {
        return "RDEDecryptionParameters(oid='$oid', publicKey='$publicKey', protectedCommand='$protectedCommand')"
    }
}