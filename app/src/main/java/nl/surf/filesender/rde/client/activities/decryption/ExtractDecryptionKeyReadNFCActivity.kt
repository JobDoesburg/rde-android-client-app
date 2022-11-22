package nl.surf.filesender.rde.client.activities.decryption

import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.widget.Toast
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.sf.scuba.smartcards.CardService
import net.sf.scuba.util.Hex
import nl.surf.filesender.rde.data.RDEDecryptionParameters
import nl.surf.filesender.rde.RDEDocument
import nl.surf.filesender.rde.client.activities.general.ReadNFCActivity
import nl.surf.filesender.rde.client.activities.enrollment.EnrollmentResultActivity

class ExtractDecryptionKeyReadNFCActivity : ReadNFCActivity() {

    lateinit var decryptionParameters: RDEDecryptionParameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val decryptionParamsData = intent.getStringExtra("data")!!
        try {
            decryptionParameters = Json.decodeFromString(decryptionParamsData)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid enrollment parameters", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        extractDecryptionKey(decryptionParameters)
    }

    private fun extractDecryptionKey(decryptionParameters: RDEDecryptionParameters) {
        val document = RDEDocument(documentName!!, bacKey!!)

        val isoDep = IsoDep.get(tag)
        isoDep.timeout = 100000 // Long because of debugging, remove in production
        val cardService = CardService.getInstance(isoDep)

        document.init(cardService)
        document.open()
        val decryptionKey = document.decrypt(decryptionParameters)

        val resultData = Hex.toHexString(decryptionKey)
        val intent = Intent(this, DecryptionResultActivity::class.java)
        if (receivedIntentExtras != null) {
            intent.putExtras(receivedIntentExtras!!)
        }
        intent.putExtra("result_data", resultData)
        startActivity(intent)

    }

}