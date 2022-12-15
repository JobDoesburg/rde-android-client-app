package nl.surf.rde.app.common

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nl.surf.rde.app.R
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jmrtd.BACKey
import java.security.Security

open class ReadNFCActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    var bacKey: BACKey? = null

    var tag: Tag? = null

    var receivedIntentExtras: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_nfc)

        // TODO add CAN support

        receivedIntentExtras = intent.extras

        fixSecurityProviders()

        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (this.nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show()
            finish()
        }
        val mrzData = intent.extras!!["mrzData"] as DocumentMRZData
        mrzData.toBACKey()
        bacKey = mrzData.toBACKey()
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
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
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