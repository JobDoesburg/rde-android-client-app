package nl.surf.filesender.rde.client.activities.decryption

import nl.surf.filesender.rde.client.activities.general.ReadMRZActivity

class DecryptionReadMRZActivity : ReadMRZActivity() {
    override var nextActivity: Class<*> = ExtractDecryptionKeyReadNFCActivity::class.java
    // TODO documentName should be hardcoded here
}