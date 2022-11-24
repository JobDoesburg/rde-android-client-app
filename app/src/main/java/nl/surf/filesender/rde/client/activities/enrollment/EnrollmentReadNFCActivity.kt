package nl.surf.filesender.rde.client.activities.enrollment

import android.app.Activity
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sf.scuba.smartcards.CardService
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.RDEDocument
import nl.surf.filesender.rde.client.activities.general.ReadNFCActivity

class EnrollmentReadNFCActivity : ReadNFCActivity() {
    companion object {
        const val RDE_DG_ID = 14
        const val RDE_RB_LENGTH = 223
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_nfc)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        enroll()
        Toast.makeText(this, "Done", Toast.LENGTH_LONG).show()
        Log.d("EnrollmentReadNFC", "Enrollment done")
    }

    private fun enroll(){
        val document = RDEDocument(bacKey!!)

        val isoDep = IsoDep.get(tag)
        isoDep.timeout = 100000 // Long because of debugging, remove in production
        val cardService = CardService.getInstance(isoDep)

        val maskedDocumentNumber = "*******" + bacKey!!.documentNumber.substring(6, 9)
        val enrollmentDocumentName = "$documentName ($maskedDocumentNumber)"

        document.init(cardService)
        document.open()
        // TODO detect the document version and country, only use withMRZData for certain versions
        val enrollmentParams = document.enroll(enrollmentDocumentName, RDE_DG_ID, RDE_RB_LENGTH, withSecurityData = true, withMRZData = true, withFaceImage = true)
        document.close()

        val returnIntent = Intent()
        if (receivedIntentExtras != null) {
            returnIntent.putExtras(receivedIntentExtras!!)
        }
        returnIntent.putExtra("result", enrollmentParams)
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}