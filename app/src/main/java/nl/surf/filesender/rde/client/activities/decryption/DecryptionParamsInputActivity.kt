package nl.surf.filesender.rde.client.activities.decryption

import nl.surf.filesender.rde.client.activities.general.ParamsInputActivity

class DecryptionParamsInputActivity : ParamsInputActivity() {
    override var nextActivity: Class<*> = DecryptionReadMRZActivity::class.java
}