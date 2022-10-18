package nl.surf.filesender.rde.client

data class RDEKey(
    val encryptionKey : ByteArray,
    val decryptionParameters: RDEDecryptionParameters
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RDEKey

        if (!encryptionKey.contentEquals(other.encryptionKey)) return false
        if (decryptionParameters != other.decryptionParameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionKey.contentHashCode()
        result = 31 * result + decryptionParameters.hashCode()
        return result
    }
}
