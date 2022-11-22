package nl.surf.filesender.rde.client.activities.decryption

import android.app.Activity
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import net.sf.scuba.smartcards.CardService
import net.sf.scuba.util.Hex
import nl.surf.filesender.rde.data.RDEDecryptionParameters
import nl.surf.filesender.rde.RDEDocument
import nl.surf.filesender.rde.client.activities.general.ReadNFCActivity

class DecryptionReadNFCActivity : ReadNFCActivity() {

    lateinit var decryptionParameters: RDEDecryptionParameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        decryptionParameters = intent.extras!!["decryptionParams"] as RDEDecryptionParameters
        Log.d("RDE", "Decryption parameters: $decryptionParameters")
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
        try {
            document.open()
            val decryptionKey = Hex.toHexString(document.decrypt(decryptionParameters))
            val returnIntent = Intent()
            if (receivedIntentExtras != null) {
                returnIntent.putExtras(receivedIntentExtras!!)
            }
            returnIntent.putExtra("result", decryptionKey)
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        } catch (e: Exception) {
            Log.e("RDE", "Error while decrypting", e)
            Toast.makeText(this, "Error while decrypting: ${e.message}", Toast.LENGTH_LONG).show()
            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
    }

}