package nl.surf.filesender.rde.data

import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEEnrollmentParameters(val documentName: String, val caOid: String, val piccPublicKey: String, val rdeDGId: Int, val rdeRBLength: Int, val rdeDGContent: String, val securityData: String?, val mrzData: String?, val faceImageData: String?) : Serializable {
    override fun toString(): String {
        return "RDEEnrollmentParameters(documentName='$documentName', caOid='$caOid', piccPublicKey='$piccPublicKey', rdeDGId=$rdeDGId, rdeRBLength=$rdeRBLength, rdeDGContent='$rdeDGContent', securityData=$securityData, mrzData=$mrzData, faceImageData=$faceImageData)"
    }
}