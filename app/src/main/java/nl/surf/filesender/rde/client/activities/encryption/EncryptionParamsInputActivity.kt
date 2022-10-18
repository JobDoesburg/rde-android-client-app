package nl.surf.filesender.rde.client.activities.encryption

import nl.surf.filesender.rde.client.activities.general.ParamsInputActivity

class EncryptionParamsInputActivity : ParamsInputActivity() {
    override var nextActivity: Class<*> = EncryptionActivity::class.java
}