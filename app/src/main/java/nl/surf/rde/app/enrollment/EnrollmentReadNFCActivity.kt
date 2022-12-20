package nl.surf.rde.app.enrollment

import android.app.Activity
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import net.sf.scuba.smartcards.CardService
import nl.surf.rde.RDEDocument
import nl.surf.rde.app.R
import nl.surf.rde.app.common.ReadNFCActivity

class EnrollmentReadNFCActivity : ReadNFCActivity() {
    private lateinit var documentName: String
    private var withSecurityData = false
    private var withMRZData = false
    private var withFaceImageData = false
    private var disableWhenPersonalNumberFound = true

    companion object {
        const val RDE_DG_ID = 14
        const val RDE_RB_LENGTH = 223
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_nfc)

        documentName = intent.getStringExtra("documentName")!!
        withSecurityData = intent.getBooleanExtra("withSecurityData", false)
        withMRZData = intent.getBooleanExtra("withMRZData", false)
        withFaceImageData = intent.getBooleanExtra("withFaceImageData", false)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        try {
            enroll(documentName, withSecurityData, withMRZData, withFaceImageData, disableWhenPersonalNumberFound)
            Toast.makeText(this, "Done", Toast.LENGTH_LONG).show()
            Log.d("EnrollmentReadNFC", "Enrollment done")
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("EnrollmentReadNFC", "Error: ${e.message}")
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun enroll(documentName: String, withSecurityData: Boolean = true, withMRZData: Boolean = true, withFaceImage: Boolean = true, disableWhenPersonalNumberFound: Boolean = true) {
        val isoDep = IsoDep.get(tag)
        val cardService = CardService.getInstance(isoDep)

        val enrollmentDocumentName: String = if (withMRZData) {
            // The MRZ data will always contain the document number, so don't try to hide it
            "$documentName (${bacKey!!.documentNumber})"
        } else {
            val maskedDocumentNumber = "*******" + bacKey!!.documentNumber.substring(6, 9)
            "$documentName ($maskedDocumentNumber)"
        }

        val document = RDEDocument(bacKey!!, cardService)
        // TODO detect the document version and country beforehand, only use withMRZData for certain versions

        val enrollmentParams = document.enroll(enrollmentDocumentName, RDE_DG_ID, RDE_RB_LENGTH, withSecurityData, withMRZData, withFaceImage)

        if (disableWhenPersonalNumberFound && withMRZData && enrollmentParams.mrzData != null && document.dg1.mrzInfo.personalNumber != null) {
            // The MRZ data contains a privacy sensitive field that we may not process, so we remove the MRZ data
            Toast.makeText(this, "MRZ data contains personal number, cannot include MRZ data", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        // TODO the user should verify the data before continuing

        document.close()

        val returnIntent = Intent()
        if (receivedIntentExtras != null) {
            returnIntent.putExtras(receivedIntentExtras!!)
        }
        returnIntent.putExtra("result", enrollmentParams)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }
}