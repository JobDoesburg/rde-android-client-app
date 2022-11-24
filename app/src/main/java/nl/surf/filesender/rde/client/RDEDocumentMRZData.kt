package nl.surf.filesender.rde.client

import org.jmrtd.BACKey
import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEDocumentMRZData(val documentId: String, val dateOfExpiry: String, val dateOfBirth: String) : Serializable {
    fun toBACKey(): BACKey {
        return BACKey(documentId, dateOfBirth, dateOfExpiry)
    }
    override fun toString(): String {
        return "RDEDocumentMRZData(documentId='$documentId', dateOfExpiry='$dateOfExpiry', dateOfBirth='$dateOfBirth')"
    }
}
