package nl.surf.filesender.rde.data

import java.io.Serializable

@kotlinx.serialization.Serializable
data class RDEEnrollmentParameters(val n: Int, val Fid: Int, val Fcont: String, val caOid: String, val piccPublicKey: String, val documentName: String) : Serializable{
    override fun toString(): String {
        return "RDEEnrollmentParameters(n=$n, Fid=$Fid, Fcont='$Fcont', caOid='$caOid', piccPublicKey='$piccPublicKey', documentName='$documentName')"
    }
}