package nl.surf.rde.app.common

import org.jmrtd.BACKey
import java.io.Serializable

data class DocumentMRZData(val documentId: String, val dateOfExpiry: String, val dateOfBirth: String) : Serializable {
    fun toBACKey(): BACKey {
        return BACKey(documentId, dateOfBirth, dateOfExpiry)
    }
    override fun toString(): String {
        return "DocumentMRZData(documentId='$documentId', dateOfExpiry='$dateOfExpiry', dateOfBirth='$dateOfBirth')"
    }
}
