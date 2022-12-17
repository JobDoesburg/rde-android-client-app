package nl.surf.rde.app.decryption

import android.app.Activity
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import net.sf.scuba.smartcards.CardService
import nl.surf.rde.RDEDocument
import nl.surf.rde.app.common.ReadNFCActivity
import nl.surf.rde.data.RDEDecryptionParameters

class DecryptionReadNFCActivity : ReadNFCActivity() {

    private lateinit var decryptionParameters: RDEDecryptionParameters

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
        val isoDep = IsoDep.get(tag)
        val cardService = CardService.getInstance(isoDep)

        val document = RDEDocument(bacKey!!, cardService)

        try {
            val retrievedKey = document.retrieveSecretKey(decryptionParameters)
            document.close()

            val returnIntent = Intent()
            returnIntent.putExtra("result", retrievedKey)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        } catch (e: Exception) {
            Log.e("RDE", "Error while decrypting", e)
            Toast.makeText(this, "Error while decrypting: ${e.message}", Toast.LENGTH_LONG).show()
            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
        }
    }

}