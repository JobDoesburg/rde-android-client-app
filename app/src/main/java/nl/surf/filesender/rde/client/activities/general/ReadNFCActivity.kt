package nl.surf.filesender.rde.client.activities.general

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import nl.surf.filesender.rde.client.R
import nl.surf.filesender.rde.data.RDEDocumentMRZData
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.BACKey
import java.security.Security

open class ReadNFCActivity : AppCompatActivity() {
    var nfcAdapter: NfcAdapter? = null
    var bacKey: BACKey? = null
    var documentName: String? = null

    var tag: Tag? = null

    var receivedIntentExtras: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_nfc)

        receivedIntentExtras = intent.extras

        fixSecurityProviders()

        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (this.nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show()
            finish()
        }
        val mrzData = intent.extras!!["mrzData"] as RDEDocumentMRZData
        mrzData.toBACKey()
        bacKey = mrzData.toBACKey()

        documentName = intent.getStringExtra("document_name")
        if (documentName == null) {
            documentName = "unknown"
        }
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
        Security.insertProviderAt(BouncyCastleProvider(), 0)
    }

}