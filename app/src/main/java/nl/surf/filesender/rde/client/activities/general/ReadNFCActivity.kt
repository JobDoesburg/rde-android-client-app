package nl.surf.filesender.rde.client.activities.general

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.BACKey
import java.security.Security

open class ReadNFCActivity : AppCompatActivity() {
    var nfcAdapter: NfcAdapter? = null
    var bacKey: BACKey? = null
    var documentName: String? = null

    var tag: Tag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_nfc)

        fixSecurityProviders()


        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (this.nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show()
            finish()
        }
        bacKey = getBACKeyFromIntent(intent!!)
        documentName = intent.getStringExtra("document_name")
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TECH_DISCOVERED != intent!!.action) return

        tag = intent.extras!!.getParcelable(NfcAdapter.EXTRA_TAG)
            ?: return

        Toast.makeText(this, "Keep the document steady while reading", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show()
            finish()
        }

        val intent = Intent(this, this.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(
            this,
            pendingIntent,
            null,
            arrayOf(arrayOf("android.nfc.tech.IsoDep"))
        )
    }

    private fun fixSecurityProviders() {
        Security.removeProvider("BC")
        Security.removeProvider("AndroidOpenSSL")
        Security.insertProviderAt(BouncyCastleProvider(), 0)
    }

    private fun getBACKeyFromIntent(intent: Intent): BACKey? {
        val documentId = intent.extras!!.getString("document_id")
        val dateOfBirth = intent.extras!!.getString("date_of_birth")
        val dateOfExpiry = intent.extras!!.getString("date_of_expiry")
        return try {
            BACKey(documentId, dateOfBirth, dateOfExpiry)
        } catch (e: Exception) {
            Log.e("EnrollmentReadNFC", "Failed to create BACKey", e)
            Toast.makeText(
                applicationContext,
                "Data invalid", Toast.LENGTH_SHORT
            ).show()
            finishActivity(RESULT_CANCELED);
            null
        }
    }

}