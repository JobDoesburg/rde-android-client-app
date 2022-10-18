package nl.surf.filesender.rde.client.activities.enrollment

import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sf.scuba.smartcards.CardService
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.client.RDEDocument
import nl.surf.filesender.rde.client.activities.general.ReadNFCActivity

class EnrollmentReadNFCActivity : ReadNFCActivity() {
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
        val document = RDEDocument(documentName!!, bacKey!!)

        val isoDep = IsoDep.get(tag)
        isoDep.timeout = 100000 // Long because of debugging, remove in production
        val cardService = CardService.getInstance(isoDep)

        document.init(cardService)
        document.open()
        val enrollmentParams = document.enroll()

        val resultData = Json.encodeToString(enrollmentParams)
        val intent = Intent(this, EnrollmentResultActivity::class.java)
        intent.putExtra("result_data", resultData)
        startActivity(intent)
    }
}