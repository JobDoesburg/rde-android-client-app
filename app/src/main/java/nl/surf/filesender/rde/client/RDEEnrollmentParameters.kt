package nl.surf.filesender.rde.client

import kotlinx.serialization.Serializable

@Serializable
data class RDEEnrollmentParameters(val n: Int, val Fid: Int, val Fcont: ByteArray, val caOid: String, val piccPublicKey: ByteArray, val documentName: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RDEEnrollmentParameters

        if (n != other.n) return false
        if (Fid != other.Fid) return false
        if (!Fcont.contentEquals(other.Fcont)) return false
        if (caOid != other.caOid) return false
        if (!piccPublicKey.contentEquals(other.piccPublicKey)) return false
        if (documentName != other.documentName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = n
        result = 31 * result + Fid
        result = 31 * result + Fcont.contentHashCode()
        result = 31 * result + caOid.hashCode()
        result = 31 * result + piccPublicKey.contentHashCode()
        result = 31 * result + documentName.hashCode()
        return result
    }

    override fun toString(): String {
        return "RDEEnrollmentParameters(n=$n, Fid=$Fid, Fcont=${Fcont.contentToString()}, caOid='$caOid', piccPublicKey=${piccPublicKey.contentToString()}, documentName='$documentName')"
    }
}
