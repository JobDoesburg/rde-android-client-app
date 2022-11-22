package nl.surf.filesender.rde

import org.jmrtd.PassportService

object RDEDocumentConfig {
    const val MAX_BLOCK_SIZE = PassportService.DEFAULT_MAX_BLOCKSIZE
    const val TRANCEIVE_LENGTH_FOR_SECURE_MESSAGING = PassportService.NORMAL_MAX_TRANCEIVE_LENGTH
    const val USE_PACE_INSTEAD_OF_BAC = true
    const val SFI_ENABLED = true
    const val DG_ID_FOR_RDE = PassportService.SFI_DG14.toInt() // The datagroup to use for RDE, probably you want DG14 because it does not contain privacy sensitive data
}