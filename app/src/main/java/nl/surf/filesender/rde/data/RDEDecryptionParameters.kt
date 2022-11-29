package nl.surf.filesender.rde.data

import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEDecryptionParameters (
    val documentName: String,
    val caOID: String,
    val pcdPublicKey: String,
    val protectedCommand: String,
) : Serializable {
    override fun toString(): String {
        return "RDEDecryptionParameters(documentName: '$documentName', caOID='$caOID', pcdPublicKey='$pcdPublicKey', protectedCommand='$protectedCommand')"
    }
}